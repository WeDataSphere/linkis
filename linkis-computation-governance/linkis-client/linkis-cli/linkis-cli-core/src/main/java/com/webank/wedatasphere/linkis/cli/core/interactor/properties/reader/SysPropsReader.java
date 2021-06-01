/*
 * Copyright 2019 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.linkis.cli.core.interactor.properties.reader;

import com.webank.wedatasphere.linkis.cli.common.constants.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


/**
 * @program: linkis-cli
 * @description:
 * @create: 2021/02/24 19:37
 */
public class SysPropsReader implements PropertiesReader {
    private static final Logger logger = LoggerFactory.getLogger(SysPropsReader.class);
    private String propsId = CommonConstants.SYSTEM_PROPERTIES_IDENTIFIER;
    private String propsPath = "SYSTEM";


    @Override
    public String getPropsId() {
        return propsId;
    }

    @Override
    public PropertiesReader setPropsId(String identifier) {
        this.propsId = identifier;
        return this;
    }

    @Override
    public String getPropsPath() {
        return propsPath;
    }

    @Override
    public PropertiesReader setPropsPath(String propertiesPath) {
        this.propsPath = propertiesPath;
        return this;
    }


    @Override
    public Properties getProperties() {
        checkInit();
        return System.getProperties();
    }

    @Override
    public void checkInit() {

    }
}