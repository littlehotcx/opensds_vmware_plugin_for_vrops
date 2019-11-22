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

package org.opensds.ecosystem.adapter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.opensds.ecosystem.adapter.model.StorData;
import org.opensds.ecosystem.adapter.model.LoadDataFactory;
import org.opensds.ecosystem.adapter.model.meta.StorModel;
import org.opensds.ecosystem.adapter.model.meta.ResourceModel;
import org.opensds.ecosystem.adapter.rest.GetRestData;
import org.opensds.ecosystem.adapter.thread.ConnectionResults;
import org.opensds.ecosystem.adapter.util.AdapterStringUtil;
import org.opensds.ecosystem.adapter.util.OpenSDSStorAdapterUtil;
import org.opensds.ecosystem.adapter.vcenter.VCenterManager;
import org.opensds.ecosystem.adapter.vcenter.model.FSlun;
import org.opensds.ecosystem.adapter.vcenter.model.DataStore;
import org.opensds.ecosystem.adapter.vcenter.model.VMware;
import org.opensds.ecosystem.adapter.vcenter.model.Naslun;
import org.opensds.ecosystem.adapter.vcenter.model.VCenterModule;
import org.opensds.storage.conection.rest.domain.ConnectionData;
import org.opensds.storage.conection.sftp.SftpAccount;
import org.opensds.storage.extracdata.PerfStatHisFileInfo;
import org.opensds.storage.extracdata.PerfStatHisFileProxy;
import org.opensds.storage.extracdata.util.ProductModeMaps;
import org.opensds.storage.extracdata.util.VerifyUtil;
import com.integrien.alive.common.adapter3.AdapterBase;
import com.integrien.alive.common.adapter3.DiscoveryParam;
import com.integrien.alive.common.adapter3.DiscoveryResult;
import com.integrien.alive.common.adapter3.DiscoveryResult.StateChangeEnum;
import com.integrien.alive.common.adapter3.IdentifierCredentialProperties;
import com.integrien.alive.common.adapter3.MetricData;
import com.integrien.alive.common.adapter3.MetricKey;
import com.integrien.alive.common.adapter3.Relationships;
import com.integrien.alive.common.adapter3.ResourceKey;
import com.integrien.alive.common.adapter3.ResourceStatus;
import com.integrien.alive.common.adapter3.TestParam;
import com.integrien.alive.common.adapter3.config.ResourceConfig;
import com.integrien.alive.common.adapter3.config.ResourceIdentifierConfig;
import com.integrien.alive.common.adapter3.describe.AdapterDescribe;
import com.integrien.alive.common.util.CommonConstants.ResourceStateEnum;
import com.integrien.alive.common.util.CommonConstants.ResourceStatusEnum;
import com.vmware.vcops.common.l10n.LocalizedMsg;
import com.vmware.vim25.mo.ServiceInstance;

public class OpenSDSStorAdapter extends AdapterBase {
    // ��־�ռ���
    private final Logger logger;

    // ����������
    private OpenSDSStorAdapterUtil adapterUtil;

    private GetRestData getRestData;

    /**
     * Ĭ���๹����
     */
    public OpenSDSStorAdapter() {
        this(null, null);
    }

    /**
     * ���������๹��������ʼ��������ʵ�����Ƽ�ʵ��ID
     *
     * @param instanceName ������ʵ������
     * @param instanceId   ������ʵ��ID
     */
    public OpenSDSStorAdapter(String instanceName, Integer instanceId) {
        super(instanceName, instanceId);
        logger = loggerFactory.getLogger(OpenSDSStorAdapter.class);
        adapterUtil = new OpenSDSStorAdapterUtil(loggerFactory);
    }

    /**
     * �洢�����������ļ��������ļ��ж������ռ�����������Ϣ����Ӧ�Ķ��������ļ�describe.xml
     * vRealize��ܻ��Զ������������ļ����������ļ��еĶ����Զ�������vRealize��ʶ��Ķ����ṩ���ϲ����ʹ��
     *
     * @return adapterDescribe ���ö���������Ϣ
     */
    public AdapterDescribe onDescribe() {
        logger.info("Inside onDescribe method of OpenSDSStorAdapter class.");
        return adapterUtil.createAdapterDescribe();
    }

    /**
     * ��Ӧǰ̨�ֹ��ռ��������û�����ռ���ť����ʼִ���ռ�������
     * ϵͳ�����ռ����ĺ�˴洢�����ܺ�������Ϣ
     *
     * @param discParam ǰ̨���õĴ洢���ӵ�������Ϣ
     * @return discoveryResult �ֹ��ռ����
     */
    public DiscoveryResult onDiscover(DiscoveryParam discParam) {
        logger.info("Inside onDiscover method of OpenSDSStorAdapter class.");
        DiscoveryResult discoveryResult = new DiscoveryResult(
                discParam.getAdapterInstResource());

        return discoveryResult;
    }

    /**
     * �µ�������ʵ������ʱ���ã���������������ʵ��
     * ʵ�����ڼ��ͬ��˴洢���е����ӹ�ϵ
     *
     * @param resStatus           ��Դ״̬����
     * @param adapterInstResource ʵ�����ö���
     */
    public void onConfigure(ResourceStatus resStatus,
                            ResourceConfig adapterInstResource) {
        logger.info("Inside onConfigure method of OpenSDSStorAdapter class.");
        final IdentifierCredentialProperties prop = new IdentifierCredentialProperties(
                loggerFactory, adapterInstResource);
        String userName = String
                .valueOf(prop.getCredential(Constants.USER_NAME));
        String userPassWord = String
                .valueOf(prop.getCredential(Constants.USER_PWD));
        String arrayHost = prop.getIdentifier(Constants.ARRAY_HOST);
        String certificatePath = prop.getIdentifier(Constants.CERTIFICATE_PATH);

        ConnectionData connectionData = new ConnectionData();
        connectionData.setUsername(userName);
        connectionData.setPassword(userPassWord);
        connectionData.setCertificateFilePath(certificatePath);
        connectionData.setStrictCheckHostName(Constants.NO_CHECK_DOMAIN);
        // ��ѯҵ��I�У�������һ��ҵ��ɣе�½�ɹ�����Ϊ�ͺ������״̬Ϊ������״̬

        ConnectionResults connectionResults =
                new ConnectionResults(connectionData, arrayHost, logger);
        boolean connectFalg = connectionResults.getConnectionResults();

        // ���Ϊ������״̬����������������״̬Ϊ���ڽ�����״̬
        if (!connectFalg) {
            logger.error("The input IP can not be connected!");
            resStatus.setStatus(ResourceStatusEnum.RESOURCE_STATUS_DOWN);
            adapterInstResource
                    .setState(ResourceStateEnum.RESOURCE_STATE_FAILED);
        }
    }

    /**
     * ��Ӧǰ̨���Թ��ܰ�ť�����ڲ��Ժͺ�˴洢��vCenter�ķ�����ͨ��
     * ������ϸ˵����
     * 1���������õ����е��û��������룬����ͬ���еķ�����ͨ��
     * 2���������õ�vCenter���û���������ȣ�����ͬvCenter����ͨ��
     *
     * @param testParam �������Ӷ����ڰ����洢���к�vCenter����
     * @return boolean
     */
    public boolean onTest(TestParam testParam) {
        if (logger.isInfoEnabled()) {
            logger.info("OpenSDSStorAdpater|onTest|Inside onTest method of OpenSDSStorAdapter class.");
        }
        ResourceConfig adapterInstResource = testParam.getAdapterConfig()
                .getAdapterInstResource();

        final IdentifierCredentialProperties prop = new IdentifierCredentialProperties(
                loggerFactory, adapterInstResource);
        String userName = String
                .valueOf(prop.getCredential(Constants.USER_NAME));
        String userPassWord = String
                .valueOf(prop.getCredential(Constants.USER_PWD));
        String arrayHost = prop.getIdentifier(Constants.ARRAY_HOST);
        String certificatePath = prop.getIdentifier(Constants.CERTIFICATE_PATH);

        ConnectionData connectionData = new ConnectionData();
        connectionData.setUsername(userName);
        connectionData.setPassword(userPassWord);
        connectionData.setCertificateFilePath(certificatePath);
        connectionData.setStrictCheckHostName(Constants.NO_CHECK_DOMAIN);

        // ��ѯҵ��I�У�������һ��ҵ��ɣе�½�ɹ�����Ϊ�ͺ������״̬Ϊ������״̬
        String invalidIP = "";
        String[] hostIPList = arrayHost.split(Constants.ARRAY_HOST_SPLITER);
        for (int i = 0; i < hostIPList.length; i++) {
            if (logger.isInfoEnabled()) {
                logger.info("onTest|Debug iP is:" + hostIPList[i] + ".");
            }
            // ���������IP V4��V6�����򵯳���ʾ
            if (RestApiUtil.validataIPv4(hostIPList[i])
                    || RestApiUtil.validataIPv6(hostIPList[i])) {
                continue;
            } else {
                if (!StringUtils.isBlank(invalidIP)) {
                    invalidIP += ";" + hostIPList[i];
                } else {
                    invalidIP += hostIPList[i];
                }
            }
        }
        final LocalizedMsg.Namespace adapterNamespace = new LocalizedMsg.Namespace("OpenSDSStorAdapter");
        if (!StringUtils.isBlank(invalidIP)) {
            String megToUser = "The input IP:" + invalidIP + " is invalid!";
            testParam.setLocalizedMsg(LocalizedMsg._tr(adapterNamespace, megToUser));
            return false;
        }
        // �ж��û��������Ϣ�Ƿ���Ե�¼��Storage
        ConnectionResults connectionResults =
                new ConnectionResults(connectionData, arrayHost, logger);
        boolean connectFlag = connectionResults.getConnectionResults();

        if (!connectFlag) {
            testParam.setLocalizedMsg(LocalizedMsg._tr(adapterNamespace,
                    "The input IP can not be connected!"));
            return connectFlag;
        }

        // �ж�IP��ַ�Ƿ�����ͬһ����
        if (connectFlag && (arrayHost.contains(Constants.ARRAY_HOST_SPLITER))) {
            Map<String, List<String>> controllerIPList = RestApiUtil
                    .getInstance().getHostControllerIP(connectionData);
            if (!isNullMap(controllerIPList)) {
                List<String> ipV4list = controllerIPList.get("IPV4");
                List<String> ipV6list = controllerIPList.get("IPV6");
                for (int i = 0; i < hostIPList.length; i++) {
                    if (RestApiUtil.validataIPv4(hostIPList[i])) {
                        if (!ipV4list.isEmpty()
                                && !ipV4list.contains(hostIPList[i])) {
                            testParam.setLocalizedMsg(LocalizedMsg._tr(
                                    adapterNamespace,
                                    "The input IP does not belong to the same device!"));
                            return false;
                        }
                    }
                    if (RestApiUtil.validataIPv6(hostIPList[i])) {
                        String ipV6Add = hostIPList[i].replace("[", "")
                                .replace("]", "");
                        if (!ipV6list.isEmpty() && !ipV6list.contains(ipV6Add)) {
                            testParam.setLocalizedMsg(LocalizedMsg._tr(
                                    adapterNamespace,
                                    "The input IP does not belong to the same device!"));
                            return false;
                        }
                    }
                }
            } else {
                testParam.setLocalizedMsg(LocalizedMsg._tr(adapterNamespace,
                        "The input IP can not be connected!"));
                return false;
            }
        }
        if (connectFlag) {
            ConnectionData vCenterConnection = getVCenterConnectInfo(prop);
            ServiceInstance si = null;
            try {
                URL url = new URL(vCenterConnection.getHostURL());
                String vcUserName = vCenterConnection.getUsername();
                String vcPassWord = vCenterConnection.getPassword();
                si = new ServiceInstance(url, vcUserName, vcPassWord, true, 3000, 3000);
                connectFlag = true;
            } catch (MalformedURLException e) {
                logger.error("Get Host URL Error:" + e.getMessage());
                testParam.setLocalizedMsg(LocalizedMsg._tr(adapterNamespace,
                        "OpenSDS Storage connection succeed, but vCenter connection failed"));
                connectFlag = false;
            } catch (RemoteException e) {
                logger.error("VCenter connection error :"
                        + vCenterConnection.getHostURL() + ".");
                testParam.setLocalizedMsg(LocalizedMsg._tr(adapterNamespace,
                        "OpenSDS Storage connection succeed, but vCenter connection failed"));
                connectFlag = false;
            } finally {
                if (si != null) {
                    si.getServerConnection().logout();
                    if (logger.isInfoEnabled()) {
                        logger.info("VCenter connection logout :" + vCenterConnection.getHostURL() + ".");
                    }
                }
            }
        }
        return connectFlag;
    }

    /**
     * ��Ӧ��ʱ�ռ����ԣ��ռ���̨���е����ܺ��������ݣ�Ĭ��Ϊ5���ӣ���ʱ��ɵ���
     * ����ʵ�ֲ��裺
     * 1��ͨ�����õĹ���IP��ͨ��SFTP��ȡ���е����������ļ������������е�����ָ��
     * 2��ͨ��Rest�ӿڣ���ȡָ������LUN��StoragePool������ָ��
     * 3����װ�����ϵģ�ͣ��γ�����Ϊ�ṹ�����ζ���ģ�ͣ��ṩ��ǰ̨����չʾʹ��
     *
     * @param adapterInstResource ��������Դ����
     * @param monitoringResources ��������ض���
     */
    public void onCollect(ResourceConfig adapterInstResource, Collection<ResourceConfig> monitoringResources) {
        Date begin;
        begin = new Date();
        logger.info("Collect Begin.");
        logger.info("OpenSDSStorAdpater|onCollect|Inside onCollect method of OpenSDSStorAdapter class.");

        final IdentifierCredentialProperties prop = new IdentifierCredentialProperties(loggerFactory,
                adapterInstResource);
        DiscoveryResult discoveryResult = new DiscoveryResult(adapterInstResource);
        collectResult.setDiscoveryResult(discoveryResult);

        ConnectionData connectionData = new ConnectionData();

        String arrayUserPass = prop.getCredential(Constants.USER_PWD);
        String arrayHost = prop.getIdentifier(Constants.ARRAY_HOST);
        String arrayUserName = prop.getCredential(Constants.USER_NAME);
        String certificatePath = prop.getIdentifier(Constants.CERTIFICATE_PATH);

        connectionData.setUsername(arrayUserName);
        connectionData.setPassword(arrayUserPass);
        connectionData.setCertificateFilePath(certificatePath);
        connectionData.setStrictCheckHostName(Constants.NO_CHECK_DOMAIN);

        // �ж��û��������Ϣ�Ƿ���Ե�¼��Storage
        ConnectionResults connectionResults = new ConnectionResults(connectionData, arrayHost, logger);
        boolean connectFlag = connectionResults.getConnectionResults();
        if (!connectFlag) {
            logger.error("Could not connect to Storage:" + arrayHost);
            setResourceDown(adapterInstResource);
            return;
        }
        // Try to get arrayDevsn
        String arrayDevsn = null;
        String storageName = null;
        storageName = RestApiUtil.getInstance().getStorageName(connectionData);

        arrayDevsn = RestApiUtil.getInstance().getDevsnId(connectionData);
        if (storageName != null && !storageName.equals("null")) {
            if (logger.isInfoEnabled()) {
                logger.info("Get StorageName Success:" + storageName + ".");
            }
        }
        if (arrayDevsn != null && !arrayDevsn.equals("null")) {
            if (logger.isInfoEnabled()) {
                logger.info("Get ArrayDevsn Success:" + arrayDevsn + ".");
            }
        }

        // �����ȡ����storageName����arrayDevsn,��ֹ����ռ�,�ȴ���һ���ռ���ʼ
        if (StringUtils.isBlank(storageName) || storageName.equals("null") || StringUtils.isBlank(arrayDevsn)
                || arrayDevsn.equals("null")) {
            logger.error("Get StorageName or ArrayDevsn faild");
            setResourceDown(adapterInstResource);
            return;
        }
        String resourceId = String.valueOf(adapterInstResource.getResourceId());
        String fileTempPath = Constants.TEMP_FILE_PATH + Constants.FILE_SEPARATOR + storageName + "_" + resourceId;

        try {
            if (logger.isInfoEnabled()) {
                logger.info("Start check temp file path:" + URLEncoder.encode(fileTempPath, "UTF8"));
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
        checkDirectory(fileTempPath);
        StorData data = new StorData();
        int isSeriesStor = is18000seriesStor(connectionData, adapterInstResource);
        String utcTime = RestApiUtil.getInstance().getSystemUtcTime(connectionData);
        String perfPath = getPerfFilePath(connectionData);
        int arrayPort = Constants.ARRAY_PORT;

        // �ж��Ƿ�Ϊ18000ϵ�е��豸
        if (isSeriesStor == Constants.IS_18000_STOR) {
            extracDataFromArray(arrayHost, connectionData, arrayPort, fileTempPath, data, utcTime);
        } else if (isSeriesStor == Constants.IS_6800_STOR) {
            String path;
            if (isWindows()) {
                path = Constants.WINDOWS_C_DISC;
            } else {
                path = Constants.FILE_SEPARATOR + Constants.STOR_6800_FILEPATH;
            }

            compress6800DataFile(path, data, arrayDevsn);
            FileUtils fileUtils = new FileUtils(logger);
            fileUtils.deleteFile(new File(fileTempPath));
        } else {
            PerfStatHisFileInfo perfStatHisFileInfo = null;
            String[] host = arrayHost.split(Constants.ARRAY_HOST_SPLITER);
            for (int i = 0; i < host.length; i++) {
                String hostIp = host[i];
                if (AdapterStringUtil.isNull(hostIp)) {
                    continue;
                }
                if (logger.isInfoEnabled()) {
                    logger.info("Start extrac data from ip :" + hostIp + ".");
                }
                perfStatHisFileInfo = extracDataFromArray(hostIp, arrayUserName, arrayUserPass, arrayPort, fileTempPath,
                        utcTime, perfPath);
                if (perfStatHisFileInfo == null) {
                    continue;
                }
                // add data to StorData
                if (logger.isInfoEnabled()) {
                    logger.info("Start add data to StorData!");
                }
                LoadDataFactory.getInstance().add2MergeStorData(data, perfStatHisFileInfo);
            }
            if (perfStatHisFileInfo == null) {
                // �����ȡ���������ļ�������ر�
                logger.error("Unable to get performance file");
                setResourceDown(getAdapterInstResource());
            }
            if (logger.isInfoEnabled()) {
                logger.info("start to deleteFile:" + fileTempPath);
            }
            FileUtils fileUtils = new FileUtils(logger);
            fileUtils.deleteFile(new File(fileTempPath));
        }

        addData(monitoringResources, discoveryResult, connectionData, storageName, data);
        Date end = new Date();
        long diffInSecs = (end.getTime() - begin.getTime()) / Constants.MILLISECODS_TO_SECONDS;
        if (logger.isInfoEnabled()) {
            logger.info("Collect END - {} seconds:" + diffInSecs + ".");
        }
    }

    /**
     * �������
     *
     * @param monitoringResources Monitoring resources
     * @param discoveryResult Discovery result
     * @param connectionData Connection data
     * @param storageName Storage name
     * @param data Data
     */
    private void addData(Collection<ResourceConfig> monitoringResources, DiscoveryResult discoveryResult,
        ConnectionData connectionData, String storageName, StorData data) {
        getRestData = new GetRestData(this, loggerFactory);
        String arrayDevsn = data.getDeviceSn();
        ResourceKey storageResouce = addStorageData(discoveryResult,
                arrayDevsn, connectionData, monitoringResources, storageName);
        Map<String, ResourceKey> controllerResource = addControllerData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);
        Map<String, ResourceKey> diskDomainResource = addDiskDomainData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);
        Map<String, ResourceKey> diskResource = addDiskData(discoveryResult,
                data, arrayDevsn, connectionData, monitoringResources);
        Map<String, ResourceKey> fileSystemRes = addFileSystemData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);
        Map<String, ResourceKey> hostResource = addHostData(discoveryResult,
                data, arrayDevsn, connectionData, monitoringResources);
        Map<String, ResourceKey> lunResource = addLunData(discoveryResult,
                data, arrayDevsn, connectionData, monitoringResources);
        Map<String, ResourceKey> poolResource = addPoolData(discoveryResult,
                data, arrayDevsn, connectionData, monitoringResources);
        Map<String, ResourceKey> fcPortResource = addFCPortData(discoveryResult,
                data, arrayDevsn, connectionData, monitoringResources);
        Map<String, ResourceKey> iscsiPortResource = addISCSIPortData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);
        Map<String, ResourceKey> remoteResource = addRemoteReplicationData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);
        Map<String, ResourceKey> snapShotResource = addSnapShotData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);
        Map<String, ResourceKey> smartPartitionResource = addSmartPartitionData(
                discoveryResult, data, arrayDevsn, monitoringResources);
        Map<String, ResourceKey> smartQosResource = addSmartQosData(
                discoveryResult, data, arrayDevsn, connectionData,
                monitoringResources);

        Relationships rel = new Relationships();

        logger.info("Start add storageResouce!");
        List<Map<String, ResourceKey>> storageChildList =
                new ArrayList<Map<String, ResourceKey>>();
        addStorageChildMap(controllerResource, storageChildList);
        addStorageChildMap(diskDomainResource, storageChildList);
        addStorageChildMap(hostResource, storageChildList);
        addStorageChildMap(smartPartitionResource, storageChildList);
        addStorageChildMap(smartQosResource, storageChildList);
        addStorageRelationships(storageResouce, storageChildList, rel);
        logger.info("Start add lun2ControllerShip!");
        addAdapterRelationships(data.getLunData().getLun2ControllerShip(),
                controllerResource, lunResource, rel);
        logger.info("Start add pool2DiskDomainShip!");
        addAdapterRelationships(data.getPoolData().getPool2DiskDomainShip(),
                diskDomainResource, poolResource, rel);
        logger.info("Start add lun2PoolShip!");
        addAdapterRelationships(data.getLunData().getLun2PoolShip(),
                poolResource, lunResource, rel);
        logger.info("Start add fcport2ControllerShip!");
        addAdapterRelationships(data.getFcPortData().getFcport2ControllerShip(),
                controllerResource, fcPortResource, rel);
        logger.info("Start add iscsiport2ControllerShip!");
        addAdapterRelationships(
                data.getIscsiPortData().getIscsiport2ControllerShip(),
                controllerResource, iscsiPortResource, rel);
        logger.info("Start add disk2DiskDomainShip!");
        addAdapterRelationships(data.getDiskData().getDisk2DiskDomainShip(),
                diskDomainResource, diskResource, rel);
        logger.info("Start add fs2PoolShip!");
        addAdapterRelationships(data.getFileSystemData().getFs2PoolShip(),
                poolResource, fileSystemRes, rel);
        logger.info("Start add snapShot2LunShip!");
        addAdapterRelationships(data.getSnapShotModel().getSnapShot2LunShip(),
                lunResource, snapShotResource, rel);
        logger.info("Start add replication2LunShip!");
        addAdapterRelationships(
                data.getRemoteReplicationModel().getReplication2LunShip(),
                lunResource, remoteResource, rel);
        logger.info("Start add lun2QosShip!");
        addAdapterRelationships(data.getSmartQosModel().getQos2LunShip(),
                smartQosResource, lunResource, rel);
        logger.info("Start add lun2PartitionShip!");
        addAdapterRelationships(data.getLunData().getLun2PartitionShip(),
                smartPartitionResource, lunResource, rel);
        logger.info("Start add fs2ControllerShip!");
        addAdapterRelationships(data.getFileSystemData().getFs2ControllerShip(),
                controllerResource, fileSystemRes, rel);
        logger.info("Start add replication2FileSystemShip!");
        addAdapterRelationships(
                data.getRemoteReplicationModel()
                        .getReplication2FileSystemShip(), fileSystemRes,
                remoteResource, rel);
        logger.info("Start add fs2QosShip!");
        addAdapterRelationships(data.getSmartQosModel().getQos2FileSystemShip(),
                smartQosResource, fileSystemRes, rel);
        logger.info("Start add vcenter datastorage relationships!");
        getvCenterdata(rel, data, discoveryResult, arrayDevsn, monitoringResources);

        discoveryResult.addRelationships(rel);
    }

    /**
     * ���storage�Ӷ���
     *
     * @param storageChildresource storage�Ӷ�����Դ
     * @param storageChildList     storage�Ӷ��󼯺�
     */
    private void addStorageChildMap(Map<String, ResourceKey> storageChildresource,
                                    List<Map<String, ResourceKey>> storageChildList) {
        if (!isNullMap(storageChildresource)) {
            storageChildList.add(storageChildresource);
        }
    }

    /**
     * 1.Doradoc20��Doradoc21�����ļ����·���б仯��/OSM/coffer_data/omm/perf/perf_files/
     * 2.ͨ��rest�ӿڻ�ȡ�����ļ����·���������ȡʧ�ܣ�������/OSM/coffer_data/perf/perf_files/·���»�ȡ
     * @param connectionData ��������
     * @return �ļ�·��
     */
    private String getPerfFilePath(ConnectionData connectionData) {
        String perfPath = "";
        try {
            Map<String, Map<String, String>> allFilePathData = RestApiUtil.getInstance().getPerfFilePath(
                    connectionData);
            for (Map<String, String> data : allFilePathData.values()) {
                perfPath = data.get("CMO_STATISTIC_FILE");
                if (logger.isInfoEnabled()) {
                    logger.info("the rest perfPath<---->CMO_STATISTIC_FILE:" + perfPath);
                }
            }
            if (!"".equals(perfPath)) {
                perfPath = perfPath.substring(perfPath.indexOf("/"), perfPath.lastIndexOf("/") + 1);
            }
            if ("".equals(perfPath) || perfPath == null) {
                perfPath = Constants.DATA_PERF_FILE_FOLDER;
            }
            if (logger.isInfoEnabled()) {
                logger.info("get perfPath from rest:" + perfPath);
            }
        } catch (Exception e) {
            logger.error("get perfPath fail from rest:" + e.getMessage());
            perfPath = Constants.DATA_PERF_FILE_FOLDER;
        }
        return perfPath;
    }

    /**
     * ������˴洢�ṩ�������ļ�
     * ����ʵ�ֲ��裺
     * 1��ͨ���ƣԣе�½���洢����
     * 2���������µ�һ�������ļ�
     * 3����ȡ�������ļ���Ȼ���շ�����������ָ������������,���ط�����ɵ���������
     *
     * @param host     ��½����IP
     * @param uname    ��½�û���
     * @param upass    ����
     * @param port     ��½�˿�
     * @param tempPath ��ʱ�ļ�Ŀ¼
     * @return �����������
     */
    public PerfStatHisFileInfo extracDataFromArray(String host, String uname, String upass, int port, String tempPath,
        String utcTime, String perfPath) {
        SftpAccount account = new SftpAccount(host, uname, upass, port);
        SftpUtil ftpUtil = new SftpUtil(logger);

        String newDataFile = ftpUtil.getDataFileNew(account, perfPath, utcTime);
        if ("".equals(newDataFile) || newDataFile == null) {
            logger.error("the data file not exists from the path<----->" + perfPath);
            if (logger.isInfoEnabled()) {
                logger.info("start get data file: device is Doradoc20 or Doradoc21 from the path<---->"
                        + Constants.DATA_PERF_FILE_FOLDER_C20);
            }
            newDataFile = ftpUtil.getDataFileNew(account, Constants.DATA_PERF_FILE_FOLDER_C20, utcTime);
        }
        String filePath = host + Constants.SERVER_SPLITER + perfPath;
        String downLoadFile = perfPath + newDataFile;

        // �������������ļ�
        if (logger.isInfoEnabled()) {
            logger.info("Start download file:" + filePath + "|newDataFile:" + newDataFile + ".");
        }
        ftpUtil.downloadFile(account, downLoadFile, tempPath);

        try {
            String datafile = tempPath + File.separator + newDataFile;
            if (logger.isInfoEnabled()) {
                logger.info("Start extrac data file :" + URLEncoder.encode(datafile, "UTF8"));
            }
            PerfStatHisFileProxy perfStatHisFileProxy = new PerfStatHisFileProxy();
            return perfStatHisFileProxy.queryPerfStatHisFileInfoByCompress(datafile, logger);
        } catch (Exception e) {
            logger.error("Extrac data file fail!" + e.getMessage());
            logger.error("Unable to get performance file:" + host);
        }
        return null;
    }

    /**
     * ���ؽ���18000ϵ�����������ļ�
     *
     * @param host           ip��ַ
     * @param connectionData ������Ϣ
     * @param port           �˿�
     * @param tempPath       ��ʱ�ļ�Ŀ¼
     * @param data           �����ļ�����
     */
    public void extracDataFromArray(String host, ConnectionData connectionData, int port, String tempPath,
        StorData data, String utcTime) {
        SftpAccount account = new SftpAccount(host, connectionData.getUsername(), connectionData.getPassword(), port);
        SftpUtil ftpUtil = new SftpUtil(logger);

        List<String> dataFileNews = ftpUtil.getDataFileNews(account,
                Constants.IS_18000_DATA_PERF_FILE_FOLDER, utcTime);
        if (logger.isInfoEnabled()) {
            logger.info("Get dataFileNews success:" + dataFileNews + ".");
        }
        PerfStatHisFileInfo perfStatHisFileInfo = null;
        for (String newDataFile : dataFileNews) {
            String downLoadFile = Constants.IS_18000_DATA_PERF_FILE_FOLDER
                    + newDataFile;
            // �������������ļ�
            if (logger.isInfoEnabled()) {
                logger.info("Start download file:" + downLoadFile + ".");
            }
            ftpUtil.downloadFile(account, downLoadFile, tempPath);
            try {
                String datafile = tempPath + File.separator + newDataFile;
                if (logger.isInfoEnabled()) {
                    logger.info("Start extrac data file :" + URLEncoder.encode(datafile, "UTF8"));
                }
                PerfStatHisFileProxy perfStatHisFileProxy = new PerfStatHisFileProxy();
                perfStatHisFileInfo = perfStatHisFileProxy
                        .queryPerfStatHisFileInfoByCompress(datafile, logger);

            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            } catch (Exception e) {
                logger.error("Extrac data file fail!" + e.getMessage());
                // �ռ�״̬�޸�Ϊ������

                continue;
            }
            if (perfStatHisFileInfo == null) {
                continue;
            }
            LoadDataFactory.getInstance().add2MergeStorData(data,
                    perfStatHisFileInfo);
        }
        if (logger.isInfoEnabled()) {
            logger.info("start to deleteFile:" + tempPath);
        }
        FileUtils fileUtils = new FileUtils(logger);
        fileUtils.deleteFile(new File(tempPath));
        if (perfStatHisFileInfo == null) {
            logger.error("Unable to get performance file.");
            setResourceDown(getAdapterInstResource());
        }
    }

    /**
     * ��ȡvCenter����,���������ϵ��������Ӷ���͹�ϵ
     *
     * @param rel                 ��ϵģ��
     * @param data                �����ļ����ݼ���
     * @param discoveryResult     �����
     * @param devSN               �豸��
     * @param monitoringResources Ԫ�ؼ���
     */
    public void getvCenterdata(Relationships rel, StorData data, DiscoveryResult discoveryResult,
        String devSN, Collection<ResourceConfig> monitoringResources) {
        final IdentifierCredentialProperties prop = new IdentifierCredentialProperties(
                loggerFactory, getAdapterInstResource());
        ConnectionData connectionData = getVCenterConnectInfo(prop);
        ArrayList<ResourceKey> resourceKeyList = new ArrayList<ResourceKey>();

        VCenterModule vCenterData = null;
        try {
            vCenterData = VCenterManager.getvCenterData(connectionData);
        } catch (IOException e) {
            logger.error("Get getvCenterData error:" + e.getMessage());
        }
        if (vCenterData == null) {
            return;
        }
        List<DataStore> dataStores = vCenterData.getDataStore();
        for (DataStore dataStore : dataStores) {
            if (logger.isInfoEnabled()) {
                logger.info("dataStore|:" + dataStore + ".");
            }
            List<FSlun> fslunlist = dataStore.getFslun();
            Naslun naslun = dataStore.getNaslun();
            ArrayList<ResourceKey> lunList = new ArrayList<ResourceKey>();
            if (fslunlist != null || naslun != null) {
                String dateStoreName = dataStore.getDateStoreName();
                String dateStorevol = dataStore.getDateStorevol();
                ResourceKey datastoreResource = new ResourceKey(dateStoreName,
                        "Datastore", "VMWARE");
                ResourceIdentifierConfig ric1 = new ResourceIdentifierConfig(
                        "VMEntityName", dateStoreName, false);
                ResourceIdentifierConfig ric2 = new ResourceIdentifierConfig(
                        "VMEntityObjectID", dateStorevol, true);
                ResourceIdentifierConfig ric3 = new ResourceIdentifierConfig(
                        "VMEntityVCID", vCenterData.getVcid(), true);
                datastoreResource.addIdentifier(ric1);
                datastoreResource.addIdentifier(ric2);
                datastoreResource.addIdentifier(ric3);

                if (fslunlist != null && !fslunlist.isEmpty()) {
                    boolean flag = false;
                    // �ж�����������Ƿ���vCenter�ϵ���ȡ����lun
                    Map<String, ResourceKey> lunWWN = data.getLunData().getLunWWN();
                    for (FSlun fsLUN : fslunlist) {
                        String wwn = fsLUN.getWwn();
                        if (lunWWN.containsKey(wwn)) {
                            lunList.add(lunWWN.get(wwn));
                            if (logger.isInfoEnabled()) {
                                logger.info("LunList add lunWWN is:" + lunWWN.get(wwn) + ".");
                            }
                            flag = true;
                        }
                    }
                    if (flag) {
                        List<VMware> VMS = dataStore.getVms();

                        for (VMware vmware : VMS) {
                            ResourceKey resource = new ResourceKey(
                                    vmware.getVmwareVmname(),
                                    "VirtualMachine", "OpenSDSStorAdapter");
                            ResourceIdentifierConfig ric11 = new ResourceIdentifierConfig(
                                    "Id", devSN + "-VirtualMachine-"
                                    + vmware.getVmwareVal(),
                                    true);
                            ResourceIdentifierConfig ric22 = new ResourceIdentifierConfig(
                                    "Name", vmware.getVmwareVmname(), false);
                            resource.addIdentifier(ric11);
                            resource.addIdentifier(ric22);

                            if (isNewResource(resource)) {
                                discoveryResult.addResource(
                                        new ResourceConfig(resource));
                                continue;
                            }
                            ResourceConfig resourceConfig = getMonitoringResource(
                                    resource);
                            if (resourceConfig == null) {
                                continue;
                            }

                            MetricKey mk = new MetricKey();
                            mk.add("maxMksConnections");
                            Integer value = vmware.getMaxMksConnections();
                            if (value == null) {
                                value = 0;
                            }
                            MetricData maxMksData = new MetricData(mk, System.currentTimeMillis(), value);

                            MetricKey mkMaxCpu = new MetricKey();
                            mkMaxCpu.add("MaxCpuUsage");
                            Integer mkMaxCpuValue = vmware.getMaxCpuUsage();
                            if (mkMaxCpuValue == null) {
                                mkMaxCpuValue = 0;
                            }
                            MetricKey mkMaxMemory = new MetricKey();
                            mkMaxMemory.add("MaxMemoryUsage");

                            Integer maxMemoryValue = vmware
                                    .getMaxMemoryUsage();
                            if (maxMemoryValue == null) {
                                maxMemoryValue = 0;
                            }
                            MetricData maxMemoryValueData = new MetricData(
                                    mkMaxMemory, System.currentTimeMillis(),
                                    maxMemoryValue);

                            MetricKey mkMemoryOverhead = new MetricKey();
                            mkMemoryOverhead.add("MemoryOverhead");
                            Long memoryOverheadValue = vmware
                                    .getMemoryOverhead();

                            MetricData memoryOverheadData = new MetricData(
                                    mkMemoryOverhead,
                                    System.currentTimeMillis(),
                                    memoryOverheadValue);
                            MetricData maxCpuUsageData = new MetricData(mkMaxCpu, System.currentTimeMillis(),
                                    mkMaxCpuValue);

                            addMetricData(resourceConfig, maxMksData);
                            addMetricData(resourceConfig, maxCpuUsageData);
                            addMetricData(resourceConfig, maxMemoryValueData);
                            addMetricData(resourceConfig, memoryOverheadData);
                            if (!lunList.isEmpty()) {
                                lunList.add(resource);
                            }
                            resourceKeyList.add(resource);
                        }
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("The dataStore:" + dateStoreName
                                + " has no fslun or naslun at the STORE.");
                    }
                }
                if (logger.isInfoEnabled()) {
                    logger.info("Add datastore rel" + datastoreResource + "|" + lunList + ".");
                }
                if (!lunList.isEmpty()) {
                    rel.addRelationships(datastoreResource, lunList);
                }
            }
            changeResState(monitoringResources,
                    resourceKeyList,
                    "VirtualMachine");
        }
    }

    /**
     * ��ȡvCenterǰ̨������Ϣ�����ú�����װ
     *
     * @param prop ǰ̨����������Ϣ������vCenter����������Ϣ
     * @return connectionData ������װ�õĵ�vCenter������Ϣ
     */
    private ConnectionData getVCenterConnectInfo(final IdentifierCredentialProperties prop) {
        String vCenterRestUrl = String
                .valueOf(prop.getIdentifier(Constants.VCENTER_REST_URL));
        String vCenterUserName = String
                .valueOf(prop.getCredential(Constants.VCENTER_USER_NAME));
        String vCenterPW = String
                .valueOf(prop.getCredential(Constants.VCENTER_USER_PWD));
        vCenterRestUrl = Constants.ARRAY_PROTOCOL + vCenterRestUrl
                + Constants.VCENTER_URL_END;
        ConnectionData connectionData = new ConnectionData();
        connectionData.setHostURL(vCenterRestUrl);
        connectionData.setUsername(vCenterUserName);
        connectionData.setPassword(vCenterPW);

        return connectionData;
    }

    /**
     * ��Ӵ洢�����ϵ
     *
     * @param resource         �洢Ԫ��
     * @param storageChildList �洢�Ӷ��󼯺�
     * @param rel              ��ϵģ��
     */
    public void addStorageRelationships(ResourceKey resource, List<Map<String, ResourceKey>> storageChildList,
        Relationships rel) {
        if (storageChildList.isEmpty()) {
            return;
        }
        List<ResourceKey> child = new ArrayList<ResourceKey>();
        for (Map<String, ResourceKey> map : storageChildList) {
            for (ResourceKey resourceKey : map.values()) {
                child.add(resourceKey);
            }
        }
        rel.setRelationships(resource, child);
        if (logger.isInfoEnabled()) {
            logger.info("Add StorageRelation relationships : parent =" + resource + ",children=" + child + ".");
        }
    }

    /**
     * ��������������ϵ
     * ͨ����������������ϵ����
     *
     * @param relationMap          �����ϵ��
     * @param parentResourceKeyMap �������ϵ
     * @param childResourceKeyMap  �Ӷ����ϵ
     * @param rel                  ��ϵģ��
     */
    public void addAdapterRelationships(Map<String, List<String>> relationMap,
        Map<String, ResourceKey> parentResourceKeyMap,
        Map<String, ResourceKey> childResourceKeyMap, Relationships rel) {
        if (isNullMap(relationMap) || isNullMap(childResourceKeyMap)
                || isNullMap(parentResourceKeyMap)) {
            logger.error("Add Adapter Relationships error:null"
                    + "relationMap:|" + relationMap + "parentResourceKeyMap :|"
                    + parentResourceKeyMap + "childResourceKeyMap:|"
                    + childResourceKeyMap);
            return;
        }
        for (Map.Entry<String, List<String>> entry : relationMap.entrySet()) {
            if (parentResourceKeyMap.get(entry.getKey()) == null) {
                continue;
            }
            List<ResourceKey> childResourceKeyList = new ArrayList<ResourceKey>();
            List<String> child = entry.getValue();
            for (String childId : child) {
                ResourceKey childRes = childResourceKeyMap.get(childId);
                if (childRes == null) {
                    continue;
                }
                childResourceKeyList.add(childRes);
            }
            if (!childResourceKeyList.isEmpty()) {
                rel.addRelationships(parentResourceKeyMap.get(entry.getKey()),
                        childResourceKeyList);
                if (logger.isInfoEnabled()) {
                    logger.info("Add relationships : parent =" + parentResourceKeyMap.get(entry.getKey())
                            + ",children=" + childResourceKeyList + ".");
                }
            }
        }
    }

    /**
     * �����ʱĿ¼�Ƿ����
     *
     * @param fileTempPath ��ʱ�ļ�·��
     * @exception/throws Υ������  Υ��˵��
     */
    public void checkDirectory(String fileTempPath) {
        if (VerifyUtil.isEmpty(fileTempPath)) {
            return;
        }
        File file = new File(fileTempPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                logger.error("Make directory error.");
            }
        }
    }

    /**
     * ��Ӵ洢����
     *
     * @param discoveryResult     �����
     * @param devSN               �������к�
     * @param connectionData      ���Ӽ���
     * @param monitoringResources Ԫ�ؼ���
     * @param storageName         �洢����
     * @return resource �洢����
     */
    public ResourceKey addStorageData(DiscoveryResult discoveryResult, String devSN, ConnectionData connectionData,
        Collection<ResourceConfig> monitoringResources, String storageName) {
        if (StringUtils.isBlank(devSN) || StringUtils.isBlank(storageName)) {
            logger.error("OpenSDS Storage SN is empty, Please check whether the network is normal?");
            return null;
        }
        ResourceKey resource = new ResourceKey(storageName + "-" + devSN,
                "Storage", "OpenSDSStorAdapter");
        ResourceIdentifierConfig ric1 = new ResourceIdentifierConfig("Id",
                devSN + "Storage" + "", true);
        ResourceIdentifierConfig ric2 = new ResourceIdentifierConfig("Name",
                devSN + "-" + storageName, false);
        resource.addIdentifier(ric1);
        resource.addIdentifier(ric2);
        if (isNewResource(resource)) {
            discoveryResult.addResource(new ResourceConfig(resource));
            return resource;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Add storage:" + resource + ".");
        }
        long timestamp = System.currentTimeMillis();

        Map<String, Map<String, String>> allSystemData = RestApiUtil
                .getInstance().getSystemData(connectionData);
        if (logger.isInfoEnabled()) {
            logger.info("Systemdata:" + allSystemData + ".");
        }
        ResourceConfig resourceConfig = getMonitoringResource(resource);
        if (logger.isInfoEnabled()) {
            logger.info("Resource config:" + resourceConfig + ".");
        }
        if (resourceConfig == null) {
            return resource;
        }

        for (Map<String, String> data : allSystemData.values()) {
            MetricKey metricKey = new MetricKey();
            metricKey.add("SystemId");
            MetricData metricData = new MetricData(metricKey, timestamp,
                    data.get("ID"));
            addMetricData(resourceConfig, metricData);

            metricKey = new MetricKey();
            metricKey.add("SystemName");
            metricData = new MetricData(metricKey, timestamp, data.get("NAME"));
            addMetricData(resourceConfig, metricData);

            metricKey = new MetricKey();
            metricKey.add("ProductMode");
            String mode = data.get("PRODUCTMODE");
            if (mode != null && !"".equals(mode)) {
                if (Constants.PRODUCTMODE_V5R7C60.indexOf(mode) != -1) {
                    mode = data.get("productModeString");
                    if (logger.isInfoEnabled()) {
                        logger.info("in productModeString ...mode:" + mode);
                    }
                } else {
                    mode = ProductModeMaps.getValue(mode);
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("mode:" + mode);
            }
            metricData = new MetricData(metricKey, timestamp, mode);
            addMetricData(resourceConfig, metricData);

            metricKey = new MetricKey();
            metricKey.add("ProductVersion");
            String productVersion = null;
            String pointRelease = data.get("pointRelease");
            if (null != pointRelease && pointRelease.length() != 0) {
                productVersion = pointRelease;
            } else {
                productVersion = data.get("PRODUCTVERSION");
            }
            metricData = new MetricData(metricKey, timestamp,
                    productVersion);
            addMetricData(resourceConfig, metricData);

            metricKey = new MetricKey();
            metricKey.add("WWN");
            metricData = new MetricData(metricKey, timestamp, data.get("wwn"));
            addMetricData(resourceConfig, metricData);

            metricKey = new MetricKey();
            metricKey.add("TotalCapacity");
            BigDecimal num1024 = new BigDecimal("1024");
            BigDecimal num2 = new BigDecimal("2");
            String totalCapacity = data.get("TOTALCAPACITY");
            if ((totalCapacity == null || "".equals(totalCapacity)) &&
                Constants.IS18800V11.equals(data.get("PRODUCTMODE"))) {
                totalCapacity = data.get("MEMBERDISKSCAPACITY");
            }
            BigDecimal total = null;
            if (totalCapacity != null && !"".equals(totalCapacity)) {
                total = new BigDecimal(totalCapacity);
                metricData = new MetricData(metricKey, timestamp,
                        total.divide(num1024)
                                .divide(num2)
                                .divide(num1024)
                                .doubleValue());
                addMetricData(resourceConfig, metricData);
            }

            metricKey = new MetricKey();
            metricKey.add("UsedCapacity");
            String usedCapacity = data.get("USEDCAPACITY");
            BigDecimal used = null;
            if (usedCapacity != null && !"".equals(usedCapacity)) {
                used = new BigDecimal(usedCapacity);
                metricData = new MetricData(metricKey, timestamp,
                        used.divide(num1024)
                                .divide(num1024)
                                .divide(num2)
                                .doubleValue());
                addMetricData(resourceConfig, metricData);
            }

            metricKey = new MetricKey();
            metricKey.add("UserFreeCapacity");
            String userFreeCapacity = data.get("userFreeCapacity");
            BigDecimal free = null;
            if (userFreeCapacity != null && !"".equals(userFreeCapacity)) {
                free = new BigDecimal(userFreeCapacity);
                metricData = new MetricData(metricKey, timestamp,
                        free.divide(num1024)
                                .divide(num1024)
                                .divide(num2)
                                .doubleValue());
                addMetricData(resourceConfig, metricData);
            }

            metricKey = new MetricKey();
            metricKey.add("CapacityUsageRate");
            if (total != null && used != null) {
                metricData = new MetricData(metricKey, timestamp, used
                        .divide(total, Constants.SECTORS_UNIT, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(
                                Double.toString(Constants.PERCENT_CONVERSION_UNIT)))
                        .doubleValue());
                addMetricData(resourceConfig, metricData);
            }

            metricKey = new MetricKey();
            metricKey.add("CapacityFreeRate");
            if (total != null && free != null) {
                metricData = new MetricData(metricKey, timestamp, free
                        .divide(total, Constants.SECTORS_UNIT, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(
                                Double.toString(Constants.PERCENT_CONVERSION_UNIT)))
                        .doubleValue());
                addMetricData(resourceConfig, metricData);
            }
        }

        // �޸���ʷ����״̬
        for (ResourceConfig config : monitoringResources) {
            String resourceKind = config.getResourceKind();
            if (resourceKind.equalsIgnoreCase("Storage")) {
                if (!config.getResourceName()
                        .equalsIgnoreCase(resourceConfig.getResourceName())) {
                    changeResourceState(config, StateChangeEnum.NOTEXIST);
                }
            }
        }

        return resource;
    }

    /**
     * ����ռ������ж���������Ϣ
     *
     * @param discoveryResult �����
     * @param dataModel       ����ģ��
     * @param resourceKind    ����Դ����
     * @param devSN           ���к�
     * @return resList ����
     */
    public Map<String, ResourceKey> addCollectData(
            DiscoveryResult discoveryResult, StorModel dataModel,
            String resourceKind, String devSN) {
        if (StringUtils.isBlank(devSN)) {
            logger.error("OpenSDS Storage SN is empty, Please check whether the network is normal?");
            return null;
        }
        Map<String, ResourceModel> dataMap = dataModel.getDataList();
        Map<String, ResourceKey> resList = new HashMap<String, ResourceKey>();

        for (Map.Entry<String, ResourceModel> entry : dataMap.entrySet()) {
            ResourceModel resData = entry.getValue();
            String resName = resData.getResourceName();
            String resId = resData.getResourceId();
            if (StringUtils.isBlank(resId)) {
                logger.error("resId id null");
                continue;
            }
            if (StringUtils.isBlank(resName)) {
                resName = resId;
                resData.setResourceName(resId);
                if (logger.isInfoEnabled()) {
                    logger.info("resName is null The resourceKind is:" + resourceKind);
                }
            }

            ResourceKey resource = new ResourceKey(resName, resourceKind,
                    "OpenSDSStorAdapter");
            ResourceIdentifierConfig ric1 = new ResourceIdentifierConfig("Id",
                    devSN + "-" + resourceKind + "-" + resId, true);
            ResourceIdentifierConfig ric2 = new ResourceIdentifierConfig("Name",
                    resName, false);

            resource.addIdentifier(ric1);
            resource.addIdentifier(ric2);
            resList.put(resId, resource);

            Map<Integer, Integer> resDataMap = resData.getResourceData();

            if (isNewResource(resource)) {
                discoveryResult.addResource(new ResourceConfig(resource));
                continue;
            }
            ResourceConfig resourceConfig = getMonitoringResource(resource);
            if (resourceConfig == null) {
                continue;
            }
            if (resourceConfig.getResourceId() != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("ResourceConfig.getResourceId():" + resourceConfig.getResourceId() + ".");
                }
            } else {
                logger.error("ResourceConfig.getResourceId is null.");
            }

            long timestamp = System.currentTimeMillis();
            for (Map.Entry<Integer, Integer> kv : resDataMap.entrySet()) {
                MetricKey metricKey = new MetricKey();
                if (logger.isInfoEnabled()) {
                    logger.info("Kind=" + resourceKind + ",resName=" + resName
                            + ",key=" + kv.getKey() + ",value=" + kv.getValue() + ".");
                }
                metricKey.add(kv.getKey() + "");

                double value = kv.getValue();

                MetricData metricData = new MetricData(metricKey, timestamp,
                        value);
                if (logger.isInfoEnabled()) {
                    logger.info("The ture value:" + value);
                }
                if (logger.isInfoEnabled()) {
                    logger.info("The Metric key is:" + kv.getKey() + "value:"
                            + kv.getValue() + "resourceKind:" + resourceKind
                            + "resid" + resId + "resource" + resourceConfig
                            + "resConf.getResourceId():"
                            + resourceConfig.getResourceId());
                }
                if (logger.isInfoEnabled()) {
                    logger.info("ResourceKind:" + resourceKind
                            + "getAdapterConfigurationName()"
                            + resourceConfig.getAdapterConfigurationName() + ".");
                }

                addMetricData(resourceConfig, metricData);
            }
        }
        return resList;
    }

    /**
     * ���Controller����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return controllerRes���󼯺�
     */
    public Map<String, ResourceKey> addControllerData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of controller!");
        Map<String, ResourceKey> controllerRes = addCollectData(discoveryResult,
                data.getControllerData(),
                "Controller",
                devSN);
        if (controllerRes == null) {
            return null;
        }
        changeResState(monitoringResources, controllerRes, "Controller");
        return controllerRes;
    }

    /**
     * ���DiskDomain����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return diskdomainRes���󼯺�
     */
    public Map<String, ResourceKey> addDiskDomainData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of diskdomain!");
        Map<String, ResourceKey> diskdomainRes = addCollectData(discoveryResult,
                data.getDiskDomainData(),
                "DiskDomain",
                devSN);
        if (diskdomainRes == null) {
            return null;
        }
        getRestData.addDiskdomainDataByRest(connectionData, diskdomainRes);
        changeResState(monitoringResources, diskdomainRes, "DiskDomain");
        return diskdomainRes;
    }

    /**
     * ���FCPort����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return fcPortRes���󼯺�
     */
    public Map<String, ResourceKey> addFCPortData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of fcport!");
        Map<String, ResourceKey> fcPortRes = addCollectData(discoveryResult,
                data.getFcPortData(),
                "FCPort",
                devSN);
        if (fcPortRes == null) {
            return null;
        }
        getRestData.addFCPortDataByRest(connectionData,
                fcPortRes,
                data.getFcPortData());
        changeResState(monitoringResources, fcPortRes, "FCPort");
        return fcPortRes;
    }

    /**
     * ���ISCSIPort����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return iscsiPortRes���󼯺�
     */
    public Map<String, ResourceKey> addISCSIPortData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of iscsiport!");
        Map<String, ResourceKey> iscsiPortRes = addCollectData(discoveryResult,
                data.getIscsiPortData(),
                "ISCSIPort",
                devSN);
        if (iscsiPortRes == null) {
            return null;
        }
        getRestData.addiSCSIPortDataByRest(connectionData,
                iscsiPortRes,
                data.getIscsiPortData());
        changeResState(monitoringResources, iscsiPortRes, "ISCSIPort");
        return iscsiPortRes;
    }

    /**
     * ���SnapShot����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return snapShotRes���󼯺�
     */
    public Map<String, ResourceKey> addSnapShotData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of snapshot!");
        Map<String, ResourceKey> snapShotRes = addCollectData(discoveryResult,
                data.getSnapShotModel(),
                "SnapShot",
                devSN);
        if (snapShotRes == null) {
            return null;
        }
        getRestData.addSnapShotDataByRest(connectionData,
                snapShotRes,
                data.getSnapShotModel());
        changeResState(monitoringResources, snapShotRes, "SnapShot");
        return snapShotRes;
    }

    /**
     * ���SmartPartition����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param monitoringResources Ԫ�ؼ���
     * @return ���󼯺�
     */
    public Map<String, ResourceKey> addSmartPartitionData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of smartpartition!");
        Map<String, ResourceKey> smartPartRes = addCollectData(discoveryResult,
                data.getSmartPartitionModel(),
                "SmartPartition",
                devSN);
        if (smartPartRes == null) {
            return null;
        }
        changeResState(monitoringResources, smartPartRes, "SmartPartition");
        return smartPartRes;
    }

    /**
     * ���SmartQos����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return qosRes���󼯺�
     */
    public Map<String, ResourceKey> addSmartQosData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of smartqos!");
        Map<String, ResourceKey> qosRes = addCollectData(discoveryResult,
                data.getSmartQosModel(),
                "SmartQos",
                devSN);
        if (qosRes == null) {
            return null;
        }
        getRestData.addQosDataByRest(connectionData,
                qosRes,
                data.getSmartQosModel());
        changeResState(monitoringResources, qosRes, "SmartQos");
        return qosRes;
    }

    /**
     * ���Զ�̸�������
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return remoteRes���󼯺�
     */
    public Map<String, ResourceKey> addRemoteReplicationData(
            DiscoveryResult discoveryResult, StorData data, String devSN,
            ConnectionData connectionData,
            Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of remotereplication!");
        Map<String, ResourceKey> remoteRes = getRestData
                .addRemoteReplicationDataByRest(connectionData,
                        discoveryResult,
                        data.getRemoteReplicationModel(),
                        devSN,
                        monitoringResources);
        return remoteRes;
    }

    /**
     * ���Disk����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return diskRes���󼯺�
     */
    public Map<String, ResourceKey> addDiskData(DiscoveryResult discoveryResult,
                                                StorData data, String devSN, ConnectionData connectionData,
                                                Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of disk!");
        Map<String, ResourceKey> diskRes = addCollectData(discoveryResult,
                data.getDiskData(),
                "Disk",
                devSN);
        if (diskRes == null) {
            return null;
        }
        getRestData.addDiskDataByRest(connectionData,
                diskRes,
                data.getDiskData());
        changeResState(monitoringResources, diskRes, "Disk");
        return diskRes;
    }

    /**
     * ���fileSystem����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return fileSystemRes���󼯺�
     */
    public Map<String, ResourceKey> addFileSystemData(DiscoveryResult discoveryResult, StorData data, String devSN,
        ConnectionData connectionData, Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of filesystem!");
        Map<String, ResourceKey> fileSystemRes = addCollectData(discoveryResult,
                data.getFileSystemData(),
                "FileSystem",
                devSN);
        if (fileSystemRes == null) {
            return null;
        }
        getRestData.addFileSystemDataByRest(connectionData,
                fileSystemRes,
                data.getFileSystemData());
        changeResState(monitoringResources, fileSystemRes, "FileSystem");
        return fileSystemRes;
    }

    /**
     * ���host����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return hostRes���󼯺�
     */
    public Map<String, ResourceKey> addHostData(DiscoveryResult discoveryResult,
        StorData data, String devSN, ConnectionData connectionData, Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of host!");
        Map<String, ResourceKey> hostRes = getRestData.addHostDataByRest(
                connectionData,
                discoveryResult,
                data.getHostData(),
                devSN,
                monitoringResources);
        return hostRes;
    }

    /**
     * ���lun����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return lunRes���󼯺�
     */
    public Map<String, ResourceKey> addLunData(DiscoveryResult discoveryResult,
        StorData data, String devSN, ConnectionData connectionData, Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of lun!");
        Map<String, ResourceKey> lunRes = getRestData.addLunDataByRest(
                connectionData,
                discoveryResult,
                data.getLunData(),
                devSN,
                monitoringResources);
        return lunRes;
    }

    /**
     * ���Pool����
     *
     * @param discoveryResult     ����
     * @param data                �����ļ���������
     * @param devSN               �������к�
     * @param connectionData      ������Ϣ
     * @param monitoringResources Ԫ�ؼ���
     * @return poolRes���󼯺�
     */
    public Map<String, ResourceKey> addPoolData(DiscoveryResult discoveryResult,
        StorData data, String devSN, ConnectionData connectionData, Collection<ResourceConfig> monitoringResources) {
        logger.info("Add adapter data of pool!");
        Map<String, ResourceKey> poolRes = addCollectData(discoveryResult,
                data.getPoolData(),
                "Pool",
                devSN);
        if (poolRes == null) {
            return null;
        }
        getRestData.addPoolDataByRest(connectionData,
                poolRes,
                data.getPoolData());
        changeResState(monitoringResources, poolRes, "Pool");
        return poolRes;
    }

    /**
     * �޸���ɾ�������״̬
     *
     * @param monitoringResources ��������ض���
     * @param resourceKeyList     �����ռ���ResourceKey����
     * @param resourceKind        ��������
     */
    public void changeResState(Collection<ResourceConfig> monitoringResources,
        List<ResourceKey> resourceKeyList, String resourceKind) {
        if (resourceKeyList.isEmpty() || monitoringResources.isEmpty()) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Start to change Resource State:" + monitoringResources + ".");
        }
        List<ResourceKey> monResourceKeyList = new ArrayList<ResourceKey>();
        // ȡ�������͵Ķ���
        for (ResourceConfig monResourceConfig : monitoringResources) {
            if (monResourceConfig.getResourceKind().equalsIgnoreCase(resourceKind)) {
                ResourceKey resourceKey = monResourceConfig.getResourceKey();
                monResourceKeyList.add(resourceKey);
                for (ResourceKey resourceKey1 : resourceKeyList) {
                    if (resourceKey.equals(resourceKey1)) {
                        if (!resourceKey.getResourceName().equals(resourceKey1.getResourceName())) {
                            if (logger.isInfoEnabled()) {
                                logger.info("<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>"
                                        + resourceKey1.getResourceName());
                            }
                        }
                    }
                }
            }
        }
        // ����ʷ������ɾ������ռ����Ķ���
        if (monResourceKeyList.isEmpty()) {
            return;
        }
        monResourceKeyList.removeAll(resourceKeyList);
        if (logger.isInfoEnabled()) {
            logger.info("After monResourceKeyList is :" + monResourceKeyList + ".");
        }
        if (monResourceKeyList.isEmpty()) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<:>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                    + monResourceKeyList.size());
        }
        for (ResourceKey resourceKey : monResourceKeyList) {
            if (logger.isInfoEnabled()) {
                logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<::>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                        + resourceKey.getResourceName());
            }
        }

        // ����ɾ���Ķ���״̬���
        for (ResourceKey res : monResourceKeyList) {
            if (logger.isInfoEnabled()) {
                logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<:::>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                        + res.getResourceName());
            }
            ResourceConfig resourceConfig = getMonitoringResource(res);
            changeResourceState(resourceConfig, StateChangeEnum.NOTEXIST);
            if (logger.isInfoEnabled()) {
                logger.info("ChangeLunState: " + "resourceKind :" + resourceKind + resourceConfig + ".");
            }
        }
    }

    /**
     * �޸���ɾ�������״̬
     *
     * @param monitoringResources ��������ض���
     * @param addCollectData      �����ռ����ݶ����map����
     * @param resourceKind        ��������
     */
    private void changeResState(Collection<ResourceConfig> monitoringResources,
        Map<String, ResourceKey> addCollectData, String resourceKind) {
        if (addCollectData.isEmpty() || monitoringResources.isEmpty()) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Start to change Resource State:" + monitoringResources + ".");
        }
        ArrayList<ResourceKey> monResourceKeyList = new ArrayList<ResourceKey>();
        ArrayList<ResourceKey> resourceKeyList = new ArrayList<ResourceKey>();

        // ��������ʷ������ȡ�������͵Ķ���
        for (Map.Entry<String, ResourceKey> entry : addCollectData.entrySet()) {
            ResourceKey resourceKey = entry.getValue();
            resourceKeyList.add(resourceKey);
        }

        // �ӱ����ռ���ȡ��resourceKey
        for (ResourceConfig monResourceConfig : monitoringResources) {
            if (monResourceConfig.getResourceKind()
                    .equalsIgnoreCase(resourceKind)) {
                ResourceKey resourceKey = monResourceConfig.getResourceKey();
                monResourceKeyList.add(resourceKey);
            }
        }

        // ����ʷ������ɾ������ռ����Ķ���
        monResourceKeyList.removeAll(resourceKeyList);
        if (logger.isInfoEnabled()) {
            logger.info("after monResourceKeyList is :" + monResourceKeyList);
        }
        if (monResourceKeyList.isEmpty()) {
            return;
        }
        // ����ɾ���Ķ���״̬���
        for (ResourceKey res : monResourceKeyList) {
            ResourceConfig resourceConfig = getMonitoringResource(res);
            changeResourceState(resourceConfig, StateChangeEnum.NOTEXIST);
            if (logger.isInfoEnabled()) {
                logger.info("ChangeLunState: " + "resourceKind :" + resourceKind + resourceConfig + ".");
            }
        }
    }


    /**
     * �ж��Ƿ�Ϊ18000ϵ�е��豸
     *
     * @param connectionData      ������Ϣ����
     * @param adapterInstResource ������ʵ��
     * @return �������д�����
     */
    public int is18000seriesStor(ConnectionData connectionData, ResourceConfig adapterInstResource) {
        ArrayList<String> seriesStorList = new ArrayList<String>();
        seriesStorList.add(Constants.IS18500V11);
        seriesStorList.add(Constants.IS18500V3);
        seriesStorList.add(Constants.IS18800V12);
        seriesStorList.add(Constants.IS18800V3);

        Map<String, Map<String, String>> allSystemData = RestApiUtil
                .getInstance().getSystemData(connectionData);
        for (Map<String, String> data : allSystemData.values()) {
            String productMode = data.get("PRODUCTMODE");
            if (logger.isInfoEnabled()) {
                logger.info("PRODUCTMODE:" + productMode);
            }
            if (StringUtils.isBlank(productMode)) {
                logger.error("Unable to get system model");
                setResourceDown(adapterInstResource);
            }
            if (seriesStorList.contains(productMode)) {
                if (logger.isInfoEnabled()) {
                    logger.info("The storage type is 18800.");
                }
                return Constants.IS_18000_STOR;
            }
            if (productMode.equals(Constants.IS6800V3) ||
                    productMode.equals(Constants.IS18500V12) ||
                    productMode.equals(Constants.IS18800V11) ||
                    productMode.equals(Constants.IS18800V13) ||
                    productMode.equals(Constants.D5000V6) ||
                    productMode.equals(Constants.D5000V6_N) ||
                    productMode.equals(Constants.D6000V6) ||
                    productMode.equals(Constants.D6000V6_N) ||
                    productMode.equals(Constants.D8000V6) ||
                    productMode.equals(Constants.D8000V6_N) ||
                    productMode.equals(Constants.D18000V6) ||
                    productMode.equals(Constants.D18000V6_N) ||
                    productMode.equals(Constants.D3000V6) ||
                    productMode.equals(Constants.D5000V6_I) ||
                    productMode.equals(Constants.D6000V6_I) ||
                    productMode.equals(Constants.D8000V6_I) ||
                    productMode.equals(Constants.D18000V6_I) ||
                    productMode.equals(Constants.DORADO5300_V6) ||
                    productMode.equals(Constants.DORADO5500_V6) ||
                    productMode.equals(Constants.DORADO5600_V6) ||
                    productMode.equals(Constants.DORADO5800_V6) ||
                    productMode.equals(Constants.DORADO6800_V6) ||
                    productMode.equals(Constants.DORADO18500_V6) ||
                    productMode.equals(Constants.DORADO18800_V6)) {
                if (logger.isInfoEnabled()) {
                    logger.info("The storage type is 68000v3 or 18500v1.");
                }
                return Constants.IS_6800_STOR;
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("The storage type is opensds storage.");
        }
        return Constants.IS_OPENSDS_STOR;
    }

    /**
     * �����洢�ṩ��68000�������������ļ�
     *
     * @param path �����ļ�·��
     * @param data ���ݴ洢����
     * @param sn   �������к�
     */
    public void compress6800DataFile(String path, StorData data, String sn) {
        if (StringUtils.isBlank(path)) {
            if (logger.isInfoEnabled()) {
                logger.info("The path is null.");
            }
            setResourceDown(getAdapterInstResource());
        }
        String filePath = path + File.separator + Constants.STOR_6800_PERF_FOLDER;
        File dirFile = new File(filePath);
        if (!dirFile.isDirectory()) {
            logger.error("Unable to retrieve performance file directory.");
            setResourceDown(getAdapterInstResource());
        }
        File[] tempList = dirFile.listFiles();
        long latestFileTime = Constants.FIRST_VALUE;
        if (tempList == null) {
            return;
        }
        for (File file : tempList) {
            String fileName = file.getName();
            if (fileName.contains(sn) && fileName.endsWith(".tgz")) {
                long lastModifiedTime = file.lastModified();

                if (latestFileTime < lastModifiedTime) {
                    latestFileTime = lastModifiedTime;
                }
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("The First file date is:" + latestFileTime + ".");
        }
        PerfStatHisFileInfo perfStatHisFileInfo = null;
        for (File file : tempList) {
            String fileName = file.getName();
            if (fileName.contains(sn) && fileName.endsWith(".tgz")) {
                if (file.lastModified() < latestFileTime + Constants.ONE_MINUTE_MILLISECOND
                        && file.lastModified() > latestFileTime - Constants.ONE_MINUTE_MILLISECOND) {
                    String datafile = filePath + File.separator + fileName;
                    if (logger.isInfoEnabled()) {
                        logger.info("6800datafile:" + datafile);
                    }

                    try {
                        PerfStatHisFileProxy perfStatHisFileProxy = new PerfStatHisFileProxy();
                        perfStatHisFileInfo = perfStatHisFileProxy.queryPerfStatHisFileInfoByCompress(
                                datafile, logger);
                    } catch (Exception e) {
                        logger.error("datafile Compress error" + datafile);
                        continue;
                    }
                    if (perfStatHisFileInfo == null) {
                        continue;
                    }
                    LoadDataFactory.getInstance().add2MergeStorData(data, perfStatHisFileInfo);
                }
            }
        }
        FileUtils fileUtils = new FileUtils(logger);
        for (File file : tempList) {
            if (file.lastModified() <= latestFileTime - Constants.HALF_HOUR_MILLISECOND
                    && file.getName().length() >= Constants.FILE_NAME_MINIMUM_LENGTH) {
                fileUtils.deleteFile(file);
            }
        }
        for (File file : tempList) {
            String filename = file.getName();
            String extensionName = fileUtils.getExtensionName(filename);
            if (filename.contains(sn) && extensionName.contains("dat")) {
                fileUtils.deleteFile(file);
            }
        }

        if (perfStatHisFileInfo == null) {
            logger.error("Unable to get performance file");
            setResourceDown(getAdapterInstResource());
        }
    }

    /**
     * �ж϶����Ƿ�Ϊ��
     *
     * @param map Map
     * @return boolean
     */
    public Boolean isNullMap(Map map) {
        return map == null || map.isEmpty() ? true : false;
    }

    /**
     * �жϵ�ǰ���л����Ƿ�Ϊwindows
     *
     * @return ��windows����true, ����false
     */
    public boolean isWindows() {
        String property = System.getProperties().getProperty("os.name");
        if (property == null) {
            logger.error("Can't get OS type.");
            setResourceDown(getAdapterInstResource());
        } else if (property.toLowerCase(Locale.getDefault()).startsWith("win")) {
            return true;
        } else {
            logger.error("Can't get OS type with" + Locale.getDefault());
        }
        return false;
    }
}
