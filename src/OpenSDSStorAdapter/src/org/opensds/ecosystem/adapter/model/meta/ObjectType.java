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

package org.opensds.ecosystem.adapter.model.meta;

public enum ObjectType {
    /**
     * ������
     */
    Controller(207),
    
    /**
     * Ӳ����
     */
    DiskDomain(266),
    
    /**
     * ��Դ��
     */
    Pool(216),
    
    /**
     * ����
     */
    Disk(10),
    
    /**
     * LUN��Ϣ
     */
    Lun(11),
    
    /**
     * �ļ�ϵͳ
     */
    FileSystem(40),
    
    /**
     * FC�˿�
     */
    FCPort(212),
    
    /**
     * iSCSI�˿�
     */
    ISCSIPort(213),
    
    /**
     * Զ�̸���
     */
    RemoteReplication(263),
    
    /**
     * ������Ϣ
     */
    SmartPartition(268),
    
    /**
     * Qos��Ϣ
     */
    SmartQos(230),
    
    /**
     * ������Ϣ
     */
    LunCopy(219),
    
    /**
     * ������Ϣ
     */
    SnapShot(27),

    /**
     * SAS�˿�
     */
    SASPort(214),

    /**
     * ������Ϣ
     */
    Host(21);
    
    private int id;
    
    private ObjectType(int id) {
        this.id = id;
    }
    
    public int getId() {
        return this.id;
    }
    
    /**
     * ת���ַ���
     * @return String
     **/
    @Override
    public String toString() {
        return String.valueOf(this.id);
    }
}
