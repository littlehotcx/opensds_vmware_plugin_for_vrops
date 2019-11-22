// Copyright 2019 The OpenSDS Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License. You may obtain
// a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.

package org.opensds.ecosystem.adapter.vcenter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.opensds.ecosystem.adapter.vcenter.model.FSlun;
import org.opensds.ecosystem.adapter.vcenter.model.DataStore;
import org.opensds.ecosystem.adapter.vcenter.model.VMware;
import org.opensds.ecosystem.adapter.vcenter.model.Naslun;
import org.opensds.ecosystem.adapter.vcenter.model.VCenterModule;
import org.opensds.storage.conection.rest.domain.ConnectionData;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.HostNasVolume;
import com.vmware.vim25.HostScsiDiskPartition;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VmfsDatastoreInfo;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public final class VCenterManager {
    // ��־��¼��
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(VCenterManager.class);

    // LUN WWNУ��������ʽ
    private static final Pattern WWN_PATT = Pattern
            .compile("(?:(?:[\\da-zA-Z]{10})|(?:naa\\.))?([\\da-zA-Z]{32}|[\\da-zA-Z]{16})(?:[\\da-zA-Z]{12})?");

    /**
     * ˽�л����췽��
     */
    private VCenterManager() {
    }

    /**
     * ��vCenter��ȡLUN��FileSystem����Ϣ
     * ��ȡ��Ϣ��ϸ���裺
     * 1��ʹ�ó�ʼ�����õ��û��������������vCenter����
     * 2��ͨ��vCenter�����ṩ��API��ѯ���е�DataStore��DataStore�ϵ�VM
     * 3��ͨ��VM������Disk��Ϣ��ȡ�����̵�WWN������ʶ�������Դ�ڴ洢
     * 4��������װ�õ����е�DataStore��VM��Ϣ
     *
     * @param connData vCenter����������Ϣ
     * @return VCenterModule
     * @throws IOException ʹ��ͨ���쳣����
     * @remark create c90005773 5 Sep 2016 1.0.0
     * @see [VCenterManager��VCenterManager#getvCenterData]
     */
    public static VCenterModule getvCenterData(ConnectionData connData) throws IOException {
        URL url = new URL(connData.getHostURL());
        String userName = connData.getUsername();
        String passWord = connData.getPassword();
        ServiceInstance si = new ServiceInstance(url, userName, passWord, true);
        Folder rootFolder = si.getRootFolder();
        ManagedEntity[] managedEntity = new InventoryNavigator(rootFolder).searchManagedEntities("Datastore");
        ArrayList<DataStore> dataStoreList = new ArrayList<DataStore>();
        for (ManagedEntity operateEntity : managedEntity) {
            // ���ҵ����е�DataStore����
            DataStore DataStore = new DataStore();
            Datastore dataStore = (Datastore) operateEntity;
            DatastoreInfo dsInfo = dataStore.getInfo();
            // ��ȡ���е�DataStore��DataStore�ϵ������
            getDatastoreAndVirtualMachineInfo(dataStoreList, DataStore, dataStore, dsInfo);
            // ��ȡDataStore�Ϲ�����LUN���ļ�ϵͳ
            getLUNWWNAndFileSystemPath(DataStore, dsInfo);
        }

        VCenterModule vCenterModule = new VCenterModule();
        vCenterModule.setVcid(si.getAboutInfo().getInstanceUuid());
        vCenterModule.setDataStore(dataStoreList);

        // �˳�vCenter����
        logout(si);
        return vCenterModule;
    }

    /**
     * �ͷ�vRealize�ͻ��˺�vCenter���������
     *
     * @param si vCenter�ͻ�������ʵ��
     * @remark create c90005773 5 Sep 2016 1.0.0
     */
    private static void logout(ServiceInstance si) {
        if (si != null) {
            try {
                si.getServerConnection().logout();
            } catch (Exception e) {
                LOGGER.error("vCenter Connection logout error" + e.getMessage());
            }
        }
    }

    /**
     * ��ȡDataStor�Ϲ����Ĵ��̵�WWN�����Ϊ�ļ�ϵͳ�����ȡ�ļ�ϵͳ�Ĺ���·��
     * ��ȡ����˵����
     * 1����ȡ���е�DataStore����ѯ����ʹ�õĴ��̷���
     * 2�����Ҵ��̷������ƣ���ȡ���е�WWN
     * 3�����DataStore����ΪNAS���ͣ����ȡNAS����ķ�������host�͹����·��path
     *
     * @param DataStore vRealize�洢�洢�������ݽṹ
     * @param dsInfo      vCenter�����е�DataStore����
     */
    private static void getLUNWWNAndFileSystemPath(DataStore DataStore, DatastoreInfo dsInfo) {
        ArrayList<FSlun> lunList = new ArrayList<FSlun>();
        if (dsInfo instanceof VmfsDatastoreInfo) {
            VmfsDatastoreInfo vdsInfo = (VmfsDatastoreInfo) dsInfo;
            HostScsiDiskPartition[] extentArr = vdsInfo.getVmfs().getExtent();

            for (HostScsiDiskPartition diskPartition : extentArr) {
                String wwn = getWwnFromIdentifier(diskPartition.getDiskName());
                if (StringUtils.isNotBlank(wwn)) {
                    FSlun blockLun = new FSlun();
                    blockLun.setWwn(normalizeWwn(wwn));
                    lunList.add(blockLun);
                }
                DataStore.setFslun(lunList);
            }
        } else if (dsInfo instanceof NasDatastoreInfo) {
            HostNasVolume nasVolume = ((NasDatastoreInfo) dsInfo).getNas();
            if (null != nasVolume) {
                Naslun naslun = new Naslun();
                naslun.setNfsHost(nasVolume.getRemoteHost());
                naslun.setNfsPath(nasVolume.getRemotePath());
                DataStore.setNaslun(naslun);
            }
        } else {
            return;
        }
    }

    /**
     * ��ȡ���е�DataStore��DataStore�ϵ������
     * ��ϸ���裺
     * 1��ͨ��DataStore�����ȡ�����е�VM
     * 2��ͬ����ȡVM��������̵���������Ϣ
     *
     * @param [dataStoreList] [�Զ������ݽṹ���ڴ洢���е�DataStore����]
     * @param [DataStore]   [�Զ������ݽṹ�û���¼VM�������Ϣ]
     * @param [dataStore]     [vCenter DataStore����]
     * @param [dsInfo]        [DataStore������Ϣ����]
     * @remark create c90005773 5 Sep 2016 1.0.0
     */
    private static void getDatastoreAndVirtualMachineInfo(List<DataStore> dataStoreList, DataStore DataStore,
                                                          Datastore dataStore, DatastoreInfo dsInfo) {
        // Get VM info from vCenter
        VirtualMachine[] vms = dataStore.getVms();
        ArrayList<VMware> vmsList = new ArrayList<VMware>();
        for (VirtualMachine virtualMachine : vms) {
            VMware Vmware = new VMware();
            if (null != virtualMachine) {
                VirtualMachineConfigInfo config = virtualMachine.getConfig();
                Vmware.setVmwareVal(virtualMachine.getMOR().getVal());
                Vmware.setVmwareVmname(virtualMachine.getName());
                if (config != null) {
                    Vmware.setAnnotation(config.getAnnotation());
                    Vmware.setMaxMksConnections(config.getMaxMksConnections());
                }

                VirtualMachineRuntimeInfo runtime = virtualMachine.getRuntime();

                Vmware.setMaxCpuUsage(runtime.getMaxCpuUsage() == null
                        ? Integer.valueOf(0) : runtime.getMaxCpuUsage());
                Vmware.setMaxMemoryUsage(runtime.getMaxMemoryUsage() == null
                        ? Integer.valueOf(0) : runtime.getMaxMemoryUsage());
                Vmware.setMemoryOverhead(runtime.getMemoryOverhead() == null ? 0 : runtime.getMemoryOverhead());

                vmsList.add(Vmware);
            }
        }

        DataStore.setVms(vmsList);
        DataStore.setDateStoreName(dataStore.getName());
        DataStore.setDateStorevol(dataStore.getMOR().getVal());
        DataStore.setFreeSpace(dsInfo.getFreeSpace());
        DataStore.setMaxFileSize(dsInfo.getMaxFileSize());

        dataStoreList.add(DataStore);
    }

    /**
     * ��ȡ����������ϵ�WWN��Ϣ
     *
     * @param diskName �������������
     * @return WWN
     * @remark create c90005773 5 Sep 2016 1.0.0
     */
    public static String getWwnFromIdentifier(String diskName) {
        if (StringUtils.isNotBlank(diskName)) {
            Matcher matcher = WWN_PATT.matcher(diskName);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * ��ʽ��LUN WWN������������ַ���
     *
     * @param [wwn] [��VM�ϻ�ȡ��WWN]
     * @return [String] [disk WWN]
     */
    private static String normalizeWwn(String wwn) {
        return StringUtils.isEmpty(wwn) ? "" : wwn.replaceAll("\\:", "").toUpperCase(Locale.US);
    }

}
