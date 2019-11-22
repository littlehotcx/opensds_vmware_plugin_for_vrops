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

public class Constants {
    /**
     * vCenter����URL��ַ
     */
    public static final String VCENTER_REST_URL = "vCenterRestURL";

    /**
     * vCenter����UserName
     */
    public static final String VCENTER_USER_NAME = "vCenterUserName";

    /**
     * vCenter����Password
     */
    public static final String VCENTER_USER_PWD = "vCenterUserPwd";

    /**
     * ����������������ַ
     */
    public static final String REST_URL = "RestURL";

    /**
     * �������������û���
     */
    public static final String USER_NAME = "UserName";

    /**
     * ����������������
     */
    public static final String USER_PWD = "UserPwd";

    /**
     * ֤��·��
     */
    public static final String CERTIFICATE_PATH = "CertificatePath";

    /**
     * ���з�����IP��ַ
     */
    public static final String ARRAY_HOST = "ArrayHost";

    /**
     * ���з������豸��
     */
    public static final String ARRAY_DEVICE_SN = "DeviceSn";

    /**
     * �û���ʱ�ļ���
     */
    public static final String TEMP_FILE_PATH = System.getProperty("user.dir");

    /**
     * �ļ�������
     */
    public static final String FILE_SEPARATOR = File.separator;

    /**
     * ��������ļ�����ʱ�ļ���
     */
    public static final String TEMP_PERF_FILE_FOLDER = TEMP_FILE_PATH
            + File.separator + "TempPerf";

    /**
     * ������·���ָ���
     */
    public static final String SERVER_SPLITER = ":";

    /**
     * ���з����������ļ�Ŀ¼
     */
    public static final String DATA_PERF_FILE_FOLDER = "/OSM/coffer_data/perf/perf_files/";

    /**
     * Dorado C20���з���������Ŀ¼
     */
    public static final String DATA_PERF_FILE_FOLDER_C20 = "/OSM/coffer_data/omm/perf/perf_files/";

    /**
     * 18000ϵ�����з����������ļ�Ŀ¼
     */
    public static final String IS_18000_DATA_PERF_FILE_FOLDER = "/svp_data/PerfFile/";
    /**
     * 18500V1ϵ�����з����������ļ�Ŀ¼
     */
    public static final String IS_18500V1_DATA_PERF_FILE_FOLDER = "/svp_data/PerfFiles/";

    /**
     * Rest�����ַ
     */
    public static final String ARRAY_REST_PATH = "/deviceManager/rest";

    /**
     * Rest����Э��ͷ
     */
    public static final String ARRAY_PROTOCOL = "https://";
    /**
     * ƴװ����Vcenter��url
     */
    public static final String VCENTER_URL_END = "/sdk";
    /**
     * �������IP�ָ��
     */
    public static final String ARRAY_HOST_SPLITER = ";";
    /**
     * ����˿�
     */
    public static final String MANAGEMENT_PORT = "2";
    /**
     * ����/ҵ���Ͽ�
     */
    public static final String MANAGEMENT_OR_SERVICE_PORT = "5";
    /**
     * SFTP����˿ڣ�Ĭ��Ϊ22
     */
    public static final int ARRAY_PORT = 22;

    /**
     * Rest��������˿�
     */
    public static final Integer ARRAY_REST_PORT_V3 = 8088;

    /**
     * �������ͺ�����Rest��������˿�
     */
    public static final Integer ARRAY_REST_PORT_V1 = 443;

    /**
     * Rest������ѯ��ҳ����
     */
    public static final int REST_BATCH_PAGESIZE = 100;

    /**
     * ��ѯ��һ������
     */
    public static final int FIRST_VALUE = 0;

    /**
     * һ����
     */
    public static final int ONE_MINUTE = 60;

    /**
     * һ���Ӻ���
     */
    public static final long ONE_MINUTE_MILLISECOND = 60000;

    /**
     * ��Сʱ����
     */
    public static final long HALF_HOUR_MILLISECOND = 1800000;

    /**
     * �ļ�����С����
     */
    public static final int FILE_NAME_MINIMUM_LENGTH = 4;

    /**
     * sectors ת����λ
     */
    public static final int SECTORS_UNIT = 4;

    /**
     * ip��󳤶�
     */
    public static final int IP_MAXIMUM_LENGTH = 39;

    /**
     * Two
     */
    public static final int DOUBLE = 2;

    /**
     * �ٷֱ�ת����λ
     * Percent conversion unit
     */
    public static final double PERCENT_CONVERSION_UNIT = 100;

    /**
     * �Ƿ�����������
     */
    public static final boolean NO_CHECK_DOMAIN = false;

    /**
     * 18500v1
     */
    public static final String IS18500V11 = "46";
    /**
     * Զ�̸��Ƶ�����Ϊlun
     */
    public static final String LUN_TYPE = "11";
    /**
     * Զ�̸��Ƶ�����Ϊ �ļ�ϵͳ
     */
    public static final String FILE_SYSTEM_TYPE = "40";
    /**
     * 18500v1
     */
    public static final String IS18500V12 = "57";

    /**
     * 18800v11
     */
    public static final String IS18800V11 = "47";

    /**
     * 18800v11
     */
    public static final String IS18800V12 = "56";

    /**
     * 6800v3
     */
    public static final String IS6800V3 = "61";
    /**
     * 18800v11
     */
    public static final String IS18800V13 = "58";

    /**
     * 18500v3
     */
    public static final String IS18500V3 = "72";

    /**
     * 18800v3
     */
    public static final String IS18800V3 = "73";

    /**
     * D5000V6
     */
    public static final String D5000V6 = "811";

    /**
     * D5000V6_N
     */
    public static final String D5000V6_N = "812";

    /**
     * D6000V6
     */
    public static final String D6000V6 = "813";

    /**
     * D6000V6_N
     */
    public static final String D6000V6_N = "814";

    /**
     * D8000V6
     */
    public static final String D8000V6 = "815";

    /**
     * D8000V6_N
     */
    public static final String D8000V6_N = "816";

    /**
     * D18000V6
     */
    public static final String D18000V6 = "817";

    /**
     * D18000V6_N
     */
    public static final String D18000V6_N = "818";

    /**
     * D3000V6
     */
    public static final String D3000V6 = "819";

    /**
     * D5000V6_I
     */
    public static final String D5000V6_I = "821";

    /**
     * D6000V6_I
     */
    public static final String D6000V6_I = "822";

    /**
     * D8000V6_I
     */
    public static final String D8000V6_I = "823";

    /**
     * D18000V6_I
     */
    public static final String D18000V6_I = "824";

    /**
     * Dorado5300_V6
     */
    public static final String DORADO5300_V6 = "825";

    /**
     * Dorado5500_V6
     */
    public static final String DORADO5500_V6 = "826";

    /**
     * Dorado5600_V6
     */
    public static final String DORADO5600_V6 = "827";

    /**
     * Dorado5800_V6
     */
    public static final String DORADO5800_V6 = "828";

    /**
     * Dorado6800_V6
     */
    public static final String DORADO6800_V6 = "829";

    /**
     * Dorado18500_V6
     */
    public static final String DORADO18500_V6 = "830";

    /**
     * Dorado18800_V6
     */
    public static final String DORADO18800_V6 = "831";

    /**
     * Milliseconds to seconds
     */
    public static final long MILLISECODS_TO_SECONDS = 1000L;
    /**
     * 18000ϵ���豸
     */
    public static final int IS_18000_STOR = 18000;
    /**
     * 6800ϵ���豸
     */
    public static final int IS_6800_STOR = 6800;

    /**
     * ��6800��18000ϵ���豸
     */
    public static final int IS_OPENSDS_STOR = 0;

    /**
     * 6800�������ļ�·��
     */
    public static final String STOR_6800_FILEPATH = "storage";
    /**
     * 6800�������ļ�·��
     */
    public static final String STOR_6800_PERF_FOLDER = "OpenSDSStorAdapter";

    /**
     * windows��C��
     */
    public static final String WINDOWS_C_DISC = "c:";

    public static final String PRODUCTMODE_V5R7C60 = "120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135";
}
