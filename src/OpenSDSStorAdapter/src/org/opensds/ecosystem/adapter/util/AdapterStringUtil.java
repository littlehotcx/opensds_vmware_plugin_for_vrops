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

package org.opensds.ecosystem.adapter.util;

import org.json.JSONArray;
import org.json.JSONException;

public final class AdapterStringUtil {
    /**
     * ˽�л����췽��
     */
    private AdapterStringUtil() {
    }

    /**
     * �ַ���ת��Ϊjson
     *
     * @param str �ַ���
     * @return JSONArray
     */
    public static JSONArray parseString2JSONArray(String str) {
        try {
            return new JSONArray(str);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    /**
     * ����ַ����Ƿ�Ϊ��
     *
     * @param str �ַ���
     * @return JSONArray
     */
    public static boolean isNull(String str) {
        return str == null ? true : str.length() == 0;
    }

    /**
     * ����ַ����Ƿ�Ϊ�ǿ�
     *
     * @param str �ַ���
     * @return JSONArray
     */
    public static boolean isNotNull(String str) {
        return !isNull(str);
    }

}
