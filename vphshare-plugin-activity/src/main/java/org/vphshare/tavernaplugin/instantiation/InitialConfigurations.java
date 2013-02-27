package org.vphshare.tavernaplugin.instantiation;

import java.util.LinkedList;
import java.util.List;

import net.sf.ezmorph.bean.MorphDynaBean;
import net.sf.json.JSONObject;

public class InitialConfigurations {
    public List<InitialConfiguration> configurations;

    public List<InitialConfiguration> getInitialConfiguration() {
        return configurations;
    }

    public void setInitialConfiguration(List<InitialConfiguration> configs) {
        configurations = new LinkedList<InitialConfiguration>();
        for (Object config : configs) {
            if (config instanceof MorphDynaBean) {
                // convert from MorphDynaBean to JSONObject
                JSONObject jsonConfig = JSONObject.fromObject(config);

                // convert from JSONObject to InitialConfiguration
                InitialConfiguration convertedConfig = (InitialConfiguration) JSONObject.toBean(jsonConfig,
                        InitialConfiguration.class);

                configurations.add(convertedConfig);
            } else if (config instanceof InitialConfiguration) {
                configurations.add((InitialConfiguration) config);
            }
        }
    }
}
