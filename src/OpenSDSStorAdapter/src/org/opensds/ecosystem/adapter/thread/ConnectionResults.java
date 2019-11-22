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

package org.opensds.ecosystem.adapter.thread;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;

import org.opensds.ecosystem.adapter.Constants;
import org.opensds.storage.conection.rest.domain.ConnectionData;

public class ConnectionResults {
    private ConnectionData connectionData;
    private String arrayHost;
    private String[] hostIPList;
    private Logger logger;

    /**
     * ���췽���������Ҫ����Ϣ
     *
     * @param connectionData ���������Ϣ����
     * @param arrayHost      ��ȡ��������IP
     * @param logger         ��־����
     */
    public ConnectionResults(ConnectionData connectionData, String arrayHost, Logger logger) {
        this.connectionData = connectionData;
        this.arrayHost = arrayHost;
        this.hostIPList = arrayHost.split(Constants.ARRAY_HOST_SPLITER);
        this.logger = logger;
    }

    /**
     * ѭ�����е�ip�Ͷ˿ڣ����ò����������е���
     *
     * @return �������ͨ�򷵻�true�����򷵻�false
     */
    public boolean getConnectionResults() {
        // �ж��û��������Ϣ�Ƿ���Ե�¼��Storage
        boolean connectFlag = false;
        List<FutureTask<RestConnectModel>> list =
                new ArrayList<FutureTask<RestConnectModel>>();
        ExecutorService exec = Executors.newFixedThreadPool(hostIPList.length * Constants.DOUBLE);
        ArrayList<Integer> portList = new ArrayList<Integer>();
        portList.add(Constants.ARRAY_REST_PORT_V3);
        portList.add(Constants.ARRAY_REST_PORT_V1);
        for (int i = 0; i < hostIPList.length; i++) {
            String hostip = hostIPList[i];
            for (Integer port : portList) {
                // ��������
                FutureTask<RestConnectModel> ft =
                        new FutureTask<RestConnectModel>(
                                new TestConnectByThread(connectionData, logger, hostip,
                                        port));
                // ��ӵ�list,�������ȡ�ý��
                list.add(ft);
                // һ�����ύ���̳߳أ���ȻҲ����һ���Ե��ύ���̳߳أ�exec.invokeAll(list);
                exec.submit(ft);
            }
        }
        // ��ʼͳ�ƽ����ѭ�����е��߳�
        for (FutureTask<RestConnectModel> tempFt : list) {
            try {
                RestConnectModel restConnectModel = tempFt.get();
                if (restConnectModel.isFlag()) {
                    // ����гɹ��ĵ�½���̷߳��أ�����ֹ�����߳�
                    exec.shutdownNow();
                    String hostIP = restConnectModel.getHostIP();
                    connectFlag = true;
                    connectionData.setHostURL(restConnectModel.getUrl());
                    // DTS2018041303579 �����˻�����¼ʱ����־��ӡ
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if (logger.isInfoEnabled()) {
                        logger.info("onTest|Login Storage " + hostIP + "|Success!" + " userName: "
                                + connectionData.getUsername() + " ,Login time: " + df.format(new Date()));
                    }
                    break;
                }
            } catch (InterruptedException e) {
                logger.error("Test Connection InterruptedException:" +
                        e.getMessage());
            } catch (ExecutionException e) {
                logger.error("Test Connection ExecutionException:" +
                        e.getMessage());
            }
        }
        if (!connectFlag) {
            logger.error("The IP" + arrayHost + "can not be connected!");
            // �ر������߳�
            exec.shutdown();
        }
        return connectFlag;
    }
}
