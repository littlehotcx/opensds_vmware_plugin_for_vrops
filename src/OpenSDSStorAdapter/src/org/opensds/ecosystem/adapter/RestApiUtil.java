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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import org.opensds.storage.conection.rest.domain.ConnectionData;
import org.opensds.storage.conection.rest.exception.RestException;
import org.opensds.storage.conection.rest.operation.DeviceManager;
import org.opensds.storage.conection.rest.operation.OceanStorOperation;
import org.opensds.storage.conection.rest.operation.OperationResult;
import org.opensds.storage.extracdata.util.LogUtil;

public final class RestApiUtil {
    private static final Logger LOGGER;
    private static final Object LOCKOBJECT = new Object();
    private static RestApiUtil instance;

    private RestApiUtil() {
    }

    static {
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        try {
            String filePath = LogUtil.LOG_XML_PATH;
            File file = new File(filePath);
            context.setConfigLocation(file.toURI());
        } catch (Exception e) {
            e.getMessage();
        }
        LOGGER = context.getLogger(RestApiUtil.class.getName());
    }

    /**
     * ��ȡRestApiUtil����ʵ��
     *
     * @return �������
     */
    public static RestApiUtil getInstance() {
        synchronized (LOCKOBJECT) {
            if (instance == null) {
                instance = new RestApiUtil();
            }
            return instance;
        }
    }

    /**
     * IP V6
     *
     * @param ipv6 IPv6
     * @return boolean ���
     */
    public static boolean validataIPv6(String ipv6) {
        String ip = ipv6.replace("[", "").replace("]", "");
        boolean result = false;
        String regHex = "(\\p{XDigit}{1,4})";
        String regIPv6Full = "^(" + regHex + ":){7}" + regHex + "$";
        String regIPv6AbWithColon = "^(" + regHex + "(:|::)){0,6}" + regHex
                + "$";
        String regIPv6AbStartWithDoubleColon = "^(" + "::(" + regHex + ":){0,5}"
                + regHex + ")$";
        String regIPv6 = "^(" + regIPv6Full + ")|("
                + regIPv6AbStartWithDoubleColon + ")|(" + regIPv6AbWithColon
                + ")$";
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("regIPv6 =" + regIPv6);
        }
        if (ip.indexOf(":") != -1) {
            if (ip.length() <= Constants.IP_MAXIMUM_LENGTH) {
                String addressTemp = ip;
                int doubleColon = 0;
                while (addressTemp.indexOf("::") != -1) {
                    addressTemp = addressTemp.substring(
                            addressTemp.indexOf("::") + Constants.DOUBLE,
                            addressTemp.length());
                    doubleColon++;
                }
                if (doubleColon <= 1) {
                    result = ip.matches(regIPv6);
                }
            }
        }

        return result;
    }

    /**
     * IP V4
     *
     * @param ip IPv4
     * @return boolean ���
     */
    public static boolean validataIPv4(String ip) {
        String ipAddChecker = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

        Pattern pattern = Pattern.compile(ipAddChecker);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    /**
     * ��ȡ��¼����
     *
     * @param restHost     RestHost
     * @param uname        UserName
     * @param upass        UserPassword
     * @param restOperName RestOpername
     * @param value        Value
     * @return Map
     */
    public Map<String, String> getsingleData(String restHost, String uname,
        String upass, String restOperName, String value) {
        DeviceManager deviceManager = new DeviceManager(restHost, uname, upass);
        try {
            deviceManager.login();

            OceanStorOperation operation = new OceanStorOperation();
            operation.setOperationName(restOperName);
            operation.putOperationData("id", value);
            OperationResult operationResult = deviceManager.performAction(operation);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("======================Operation Result Start===========================");
                LOGGER.info("operationErrorCode:" + operationResult.getErrorCode());
                LOGGER.info("operationErrorDesc:" + operationResult.getErrorDescription());
                LOGGER.info("operationResultSize:" + operationResult.getResultData().size());
                LOGGER.info("operationResult:" + operationResult.getResultData());
                LOGGER.info("======================Operation Result End===========================");
            }

            deviceManager.logout();

            return operationResult.getResultData().get(0);
        } catch (RestException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("get rest request error!" + e.getMessage());
            }
        } finally {
            deviceManager.logout();
        }

        return new HashMap<String, String>();
    }

    /**
     * send rest request to get total count
     *
     * @param connectionData Connection data
     * @param restOperName Request operation name
     * @return count
     */
    private int getTotalCount(ConnectionData connectionData, String restOperName) {
        int total = 0;
        DeviceManager deviceManager = new DeviceManager(connectionData);
        try {
            deviceManager.login();
            OceanStorOperation operation = new OceanStorOperation();
            operation.setOperationName(restOperName);
            OperationResult operationResult = deviceManager.performAction(operation);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("======================Operation Result Start===========================");
                LOGGER.info("operationErrorCode:" + operationResult.getErrorCode());
                LOGGER.info("operationErrorDesc:" + operationResult.getErrorDescription());
                LOGGER.info("operationResultSize:" + operationResult.getResultData().size());
                LOGGER.info("operationResult:" + operationResult.getResultData());
                LOGGER.info("======================Operation Result End===========================");
            }

            deviceManager.logout();
            if (operationResult.getResultData().size() > 0) {
                if (operationResult.getResultData().get(0).get("COUNT")
                        == null) {
                    return 0;
                }
                total = Integer.parseInt(
                        operationResult.getResultData().get(0).get("COUNT"));
            }
        } catch (RestException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("get total count by rest error!" + e.getMessage());
            }
        } finally {
            deviceManager.logout();
        }
        return total;
    }

    /**
     * send rest request to get data list by page
     *
     * @param connectionData ��������
     * @param operName ��������
     * @param start ��ʼ�±�
     * @param limit ��ҳ��С
     * @return ��ϸ��Ϣ����
     */
    private Map<String, Map<String, String>> getDetailData(
        ConnectionData connectionData, String operName, int start, int limit, String idKey) {
        Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
        DeviceManager deviceManager = new DeviceManager(connectionData);
        try {
            deviceManager.login();
            OceanStorOperation operation = new OceanStorOperation();
            operation.setOperationName(operName);
            operation.putOperationData("range", "[" + start + "-" + limit + "]");
            OperationResult operationResult = deviceManager.performAction(operation);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("======================Operation Result Start===========================");
                LOGGER.info("operationErrorCode:" + operationResult.getErrorCode());
                LOGGER.info("operationErrorDesc:" + operationResult.getErrorDescription());
                LOGGER.info("operationResultSize:" + operationResult.getResultData().size());
                LOGGER.info("operationResult:" + operationResult.getResultData());
                LOGGER.info("======================Operation Result End===========================");
            }
            for (int i = 0; i < operationResult.getResultData().size(); i++) {
                data.put(operationResult.getResultData().get(i).get(idKey),
                        operationResult.getResultData().get(i));
            }
        } catch (RestException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("get detail data by rest error!" + e.getMessage());
            }
        } finally {
            deviceManager.logout();
        }
        return data;
    }

    /**
     * send rest request to get ID  by page
     *
     * @param connectionData Connection data
     * @param operName OperName
     * @param start    Start
     * @param limit    Limit
     * @param idKey    IDKey
     * @return ID
     */
    private String getIdData(ConnectionData connectionData, String operName, int start, int limit, String idKey) {
        // Map<String, Map<String, String>> data = new HashMap<>();
        String id = null;
        DeviceManager deviceManager = new DeviceManager(connectionData);
        try {
            deviceManager.login();

            OceanStorOperation operation = new OceanStorOperation();
            operation.setOperationName(operName);
            operation.putOperationData("range", "[" + start + "-" + limit + "]");
            OperationResult operationResult = deviceManager.performAction(operation);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("======================Operation Result Start===========================");
                LOGGER.info("operationErrorCode:" + operationResult.getErrorCode());
                LOGGER.info("operationErrorDesc:" + operationResult.getErrorDescription());
                LOGGER.info("operationResultSize:" + operationResult.getResultData().size());
                LOGGER.info("operationResult:" + operationResult.getResultData());
                LOGGER.info("======================Operation Result End===========================");
            }

            id = operationResult.getResultData().get(Constants.FIRST_VALUE).get(idKey);
        } catch (RestException e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("get detail data by rest error!" + e.getMessage());
            }
        } finally {
            deviceManager.logout();
        }
        return id;
    }

    /**
     * send rest request to get all data
     *
     * @param connectionData Connection data
     * @param pagesize PageSize
     * @param countOperName CountOperName
     * @param operName OperName
     * @return Detail Data
     */
    private Map<String, Map<String, String>> getAllData(
        ConnectionData connectionData, int pagesize, String countOperName, String operName, String idKey) {
        Map<String, Map<String, String>> detailData = new HashMap<String, Map<String, String>>();
        int totalCount = getTotalCount(connectionData, countOperName);
        int pages = totalCount % pagesize == 0 ? totalCount / pagesize
                : totalCount / pagesize + 1;
        int start;
        int limit = 0;
        for (int i = 0; i < pages; i++) {
            start = limit + 1;
            if (i == 0) {
                start = 0;
            }
            limit = start + pagesize - 1;
            detailData.putAll(getDetailData(connectionData,
                    operName,
                    start,
                    limit + 1,
                    idKey));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("get " + operName + " by page :page-" + (i + 1));
            }
        }
        return detailData;
    }

    /**
     * ��ȡ�ļ�ϵͳ����
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getFileSystemData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-filesystem-count",
                "batch-find-filesystem", "ID");
    }

    /**
     * ��ȡlun����
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getLunData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-lun-count", "batch-find-lun",
                "ID");
    }

    /**
     * ��ȡpool����
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getPoolData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-pool-count", "batch-find-pool",
                "ID");
    }

    /**
     * ��ȡӲ��������
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getDiskdomainData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-diskpool-count",
                "batch-find-diskpool", "ID");
    }

    /**
     * ��ȡ��������
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getSnapShotData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-snapshot-count",
                "batch-find-snapshot", "ID");
    }

    /**
     * ��ȡ��������
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getReplicationData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-replication-count",
                "batch-find-replication", "ID");
    }

    /**
     * ��ȡQOS����
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getQosData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-qos-count", "batch-find-qos",
                "ID");
    }

    /**
     * ��ȡ��������
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getLuncopyData(
            ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-luncopy-count",
                "batch-find-luncopy", "ID");
    }

    /**
     * ��ȡӲ��ϵͳ����
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> getDiskData(ConnectionData connectionData) {
        return getDetailData(connectionData, "get-all-disk", 0, 0, "ID");
    }

    /**
     * ��ȡϵͳ����
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> getSystemData(ConnectionData connectionData) {
        return getDetailData(connectionData, "get-all-system", 0, 0, "ID");
    }

    /**
     * ��ȡFC����
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> getFCPortData(ConnectionData connectionData) {
        return getDetailData(connectionData, "batch-find-fcport", 0, 0, "ID");
    }

    /**
     * ��ȡiscsi����
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> getiSCSIPortData(ConnectionData connectionData) {
        return getDetailData(connectionData, "batch-find-iscsiport", 0, 0, "ID");
    }

    /**
     * ��ȡsn����
     *
     * @param connectionData ��������
     * @return Data
     */
    public String getDevsnId(ConnectionData connectionData) {
        return getIdData(connectionData, "find-all-system", 0, 0, "ID");
    }

    /**
     * ��ȡsystem_utc_time
     *
     * @param connectionData ��������
     * @return Data
     */
    public String getSystemUtcTime(ConnectionData connectionData) {
        return getIdData(connectionData, "get-system_utc_time", 0, 0,
                "CMO_SYS_UTC_TIME");
    }

    /**
     * ��ȡ�����ļ��洢��λ��
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> getPerfFilePath(ConnectionData connectionData) {
        return getDetailData(connectionData, "get-perf-filepath", 0, 0, "ID");
    }

    /**
     * ��ȡ�洢��������
     *
     * @param connectionData ��������
     * @return Data
     */
    public String getStorageName(ConnectionData connectionData) {
        return getIdData(connectionData, "find-all-system", 0, 0, "NAME");
    }

    /**
     * ��ȡIP
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> geIpData(ConnectionData connectionData) {
        return getDetailData(connectionData, "find-all-eth-port", 0, 0, "ID");
    }

    /**
     * ��ȡ��������
     *
     * @param connectionData ��������
     * @param pagesize       ҳ���С
     * @return Data
     */
    public Map<String, Map<String, String>> getHostData(ConnectionData connectionData, int pagesize) {
        return getAllData(connectionData, pagesize, "get-host-count", "batch-find-host",
                "ID");
    }

    /**
     * ��ȡ����
     *
     * @param connectionData ��������
     * @return Data
     */
    public Map<String, Map<String, String>> getLifData(ConnectionData connectionData) {
        return getDetailData(connectionData, "find-all-lif", 0, 0, "ID");
    }

    /**
     * ��ȡ�����Ϲ����IP
     *
     * @param connectionData ConnectionData
     * @return Map
     */
    public Map<String, List<String>> getHostControllerIP(ConnectionData connectionData) {
        Map<String, Map<String, String>> alliSCSIPortData = RestApiUtil.getInstance().geIpData(connectionData);
        List<String> ipv4List = new ArrayList<String>();
        List<String> ipv6List = new ArrayList<String>();
        for (Map.Entry<String, Map<String, String>> entry : alliSCSIPortData.entrySet()) {
            Map<String, String> value = entry.getValue();
            String type = value.get("selectType");
            String s6800Type = value.get("LOGICTYPE");
            if (type != null && type.equals(Constants.MANAGEMENT_PORT)) {
                String ipv4 = value.get("IPV4ADDR");
                String ipv6 = value.get("IPV6ADDR");
                ipv4List.add(ipv4);
                ipv6List.add(ipv6);
            } else {
                if (s6800Type != null &&
                        (s6800Type.equals(Constants.MANAGEMENT_PORT) ||
                                s6800Type.equals(Constants.MANAGEMENT_OR_SERVICE_PORT))) {
                    String ipv4 = value.get("IPV4ADDR");
                    String ipv6 = value.get("IPV6ADDR");
                    ipv4List.add(ipv4);
                    ipv6List.add(ipv6);
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("This port: (ID:" + value.get("ID") + ") is not a management port");
                    }
                    continue;
                }
            }
        }

        // Store the IP Addresses include IPV4 and IPV6
        HashMap<String, List<String>> ipMap = new HashMap<String, List<String>>();
        if (ipv4List.isEmpty() && ipv6List.isEmpty()) {
            LOGGER.error("Unable to get management port IP");
            return ipMap;
        }

        ipMap.put("IPV4", ipv4List);
        ipMap.put("IPV6", ipv6List);

        return ipMap;
    }
}
