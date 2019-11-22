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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.opensds.storage.conection.sftp.SftpAccount;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

public class SftpUtil {
    private static final int CONNECTION_TIMEOUT = 30 * 1000;

    private final Logger logger;

    /**
     * ���췽��
     *
     * @param log ��־
     */
    public SftpUtil(Logger log) {
        logger = log;
    }

    /**
     * ��ȡSftpSession
     *
     * @param account �û��� ����ȵĶ���
     * @param jsch    JSch����
     * @return ����Session
     */
    public Session getSftpSession(SftpAccount account, JSch jsch) {
        try {
            Session session = jsch.getSession(account.getUserName(), account.getIpAddress(), account.getPort());
            session.setPassword(account.getPassword());
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sshConfig);
            session.connect(CONNECTION_TIMEOUT);
            return session;
        } catch (JSchException e) {
            String msg = "create sftp session fail ! " + e.getMessage();
            logger.error(msg);
        }
        return null;
    }

    /**
     * ��ȡͨ����Ϣ
     *
     * @param session Session
     * @return ChannelSftp
     */
    public ChannelSftp getSftpChannel(Session session) {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
        } catch (JSchException e1) {
            String msg = "get sftp channel fail! " + e1.getMessage();
            logger.error(msg);
        }
        if (channelSftp == null) {
            return null;
        }
        try {
            channelSftp.connect(CONNECTION_TIMEOUT);
        } catch (JSchException e) {
            String msg = "get sftp connect fail! " + e.getMessage();
            logger.error(msg);
        }
        return channelSftp;
    }

    /**
     * ��ȡ���ļ�
     *
     * @param account        Account
     * @param serverfilePath �ļ�·��
     * @return �ļ���
     */
    public String getDataFileNew(SftpAccount account, String serverfilePath, String utcTime) {
        JSch jsch = new JSch();
        Session session = getSftpSession(account, jsch);
        String filename = "";
        if (null == session) {
            return filename;
        }
        ChannelSftp channelSftp = getSftpChannel(session);
        if (null == channelSftp) {
            return filename;
        }
        try {
            int first = 0;

            List<LsEntry> list = channelSftp.ls(serverfilePath);
            if (list != null && !list.isEmpty()) {
                // �޳��ȴ洢ʱ���������������ļ�
                List<LsEntry> fileList = new ArrayList<LsEntry>();
                if (utcTime != null && !"".equals(utcTime)) {
                    for (LsEntry ls : list) {
                        if (ls.getAttrs().getATime() >= Long.parseLong(utcTime)) {
                            if (logger.isInfoEnabled()) {
                                logger.info("create file time: <--------------------------->:"
                                        + ls.getAttrs().getATime());
                                logger.info("utcTime: <--------------------------->:" + utcTime);
                                logger.info("remove the system_utc_time fileName <-------------------------->:"
                                        + ls.getFilename());
                            }
                            continue;
                        }
                        fileList.add(ls);
                    }
                }
                if (fileList != null && !fileList.isEmpty()) {
                    for (LsEntry ls : fileList) {
                        if (ls.getFilename().endsWith(".tgz")) {
                            if (first == 0) {
                                first = ls.getAttrs().getATime();
                            }
                            if (first <= ls.getAttrs().getATime()) {
                                filename = ls.getFilename();
                                first = ls.getAttrs().getATime();
                            }
                        }
                    }
                } else {
                    for (LsEntry ls : list) {
                        if (ls.getFilename().endsWith(".tgz")) {
                            if (first == 0) {
                                first = ls.getAttrs().getATime();
                            }
                            if (first <= ls.getAttrs().getATime()) {
                                filename = ls.getFilename();
                                first = ls.getAttrs().getATime();
                            }
                        }
                    }
                }
            }
            return filename;
        } catch (SftpException e) {
            String msg = "get new data file fail! " + e.getMessage();
            logger.error(msg);
        }
        return filename;
    }

    /**
     * ��ȡ�����ɵ������ļ��ļ���
     *
     * @param account        Account
     * @param serverfilePath �ļ�·��
     * @return �ļ����б�
     */
    public List<String> getDataFileNews(SftpAccount account, String serverfilePath, String utcTime) {
        JSch jsch = new JSch();
        Session session = getSftpSession(account, jsch);
        ArrayList<String> fileNameList = new ArrayList<String>();
        if (null == session) {
            return fileNameList;
        }
        ChannelSftp channelSftp = getSftpChannel(session);
        if (null == channelSftp) {
            return fileNameList;
        }
        try {
            int first = Constants.FIRST_VALUE;
            List<LsEntry> list = channelSftp.ls(serverfilePath);
            if (list != null && !list.isEmpty()) {
                // �޳��ȴ洢ʱ���������������ļ�
                List<LsEntry> fileList = new ArrayList<LsEntry>();
                if (utcTime != null && !"".equals(utcTime)) {
                    for (LsEntry ls : list) {
                        if (ls.getAttrs().getATime() >= Long.parseLong(utcTime)) {
                            if (logger.isInfoEnabled()) {
                                logger.info("create file time: <--------------------------->:"
                                        + ls.getAttrs().getATime());
                                logger.info("utcTime: <--------------------------->:" + utcTime);
                                logger.info("remove the system_utc_time fileName <-------------------------->:"
                                        + ls.getFilename());
                            }
                            continue;
                        }
                        fileList.add(ls);
                    }
                }
                if (fileList != null && !fileList.isEmpty()) {
                    for (LsEntry ls : fileList) {
                        if (ls.getFilename().endsWith(".tgz")) {
                            if (first == 0) {
                                first = ls.getAttrs().getATime();
                            }
                            if (first <= ls.getAttrs().getATime()) {
                                first = ls.getAttrs().getATime();
                            }
                        }
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("The First file date is:" + first);
                    }
                    for (LsEntry ls : fileList) {
                        if (ls.getFilename().length() >= Constants.FILE_NAME_MINIMUM_LENGTH) {
                            if (ls.getAttrs().getATime() < first + Constants.ONE_MINUTE
                                    && ls.getAttrs().getATime() > first - Constants.ONE_MINUTE) {
                                fileNameList.add(ls.getFilename());
                            }
                        }
                    }
                } else {
                    for (LsEntry ls : list) {
                        if (ls.getFilename().endsWith(".tgz")) {
                            if (first == 0) {
                                first = ls.getAttrs().getATime();
                            }
                            if (first <= ls.getAttrs().getATime()) {
                                first = ls.getAttrs().getATime();
                            }
                        }
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("The First file date is:" + first);
                    }
                    for (LsEntry ls : list) {
                        if (ls.getFilename().length() >= Constants.FILE_NAME_MINIMUM_LENGTH) {
                            if (ls.getAttrs().getATime() < first + Constants.ONE_MINUTE
                                    && ls.getAttrs().getATime() > first - Constants.ONE_MINUTE) {
                                fileNameList.add(ls.getFilename());
                            }
                        }
                    }
                }
            }
        } catch (SftpException e) {
            String msg = "get new data file fail! " + e.getMessage();
            logger.error(msg);
        }
        return fileNameList;
    }

    /**
     * �����ļ�
     *
     * @param account    Account
     * @param serverpath Զ��·��
     * @param localpath  ����·��
     */
    public void downloadFile(SftpAccount account, String serverpath, String localpath) {
        JSch jsch = new JSch();
        Session session = getSftpSession(account, jsch);
        if (null == session) {
            return;
        }
        ChannelSftp channelSftp = getSftpChannel(session);
        if (null == channelSftp) {
            return;
        }
        try {
            channelSftp.get(serverpath, localpath);
        } catch (SftpException e) {
            String msg = "download data file fail! " + e.getMessage();
            logger.error(msg);
        }
    }
}
