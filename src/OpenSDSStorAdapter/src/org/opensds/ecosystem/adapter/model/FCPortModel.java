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

package org.opensds.ecosystem.adapter.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensds.ecosystem.adapter.model.meta.DataType;
import org.opensds.ecosystem.adapter.model.meta.StorModel;

public class FCPortModel extends StorModel {
    // fc端口和控制器的关系对象
    private Map<String, List<String>> fcport2ControllerShip;

    /**
     * 构造方法,初始化关系对象
     */
    public FCPortModel() {
        this.fcport2ControllerShip = new HashMap<String, List<String>>();
    }

    /**
     * 初始化
     **/
    public void init() {
        getDataTypes().add(DataType.MaxIOPS.getValue());
        getDataTypes().add(DataType.BandWidth.getValue());
        getDataTypes().add(DataType.Throughput.getValue());
        getDataTypes().add(DataType.ReadBandWidth.getValue());
        getDataTypes().add(DataType.AverageReadIO.getValue());
        getDataTypes().add(DataType.ReadThroughput.getValue());
        getDataTypes().add(DataType.WriteBandWidth.getValue());
        getDataTypes().add(DataType.AverageWriteIO.getValue());
        getDataTypes().add(DataType.WirteThroughput.getValue());
        getDataTypes().add(DataType.ServiceTime.getValue());
        getDataTypes().add(DataType.AverageIOResponseTimeUs.getValue());
        getDataTypes().add(DataType.MaxIOResponseTimeUs.getValue());
        getDataTypes().add(DataType.AverageReadIOResponseTimeUs.getValue());
        getDataTypes().add(DataType.AverageWriteIOResponseTimeUs.getValue());
        getDataTypes().add(DataType.ReadPercent.getValue());
        getDataTypes().add(DataType.WritePercent.getValue());
    }

    public Map<String, List<String>> getFcport2ControllerShip() {
        return fcport2ControllerShip;
    }

    public void setFcport2ControllerShip(
            Map<String, List<String>> fcport2ControllerShip) {
        this.fcport2ControllerShip = fcport2ControllerShip;
    }

}
