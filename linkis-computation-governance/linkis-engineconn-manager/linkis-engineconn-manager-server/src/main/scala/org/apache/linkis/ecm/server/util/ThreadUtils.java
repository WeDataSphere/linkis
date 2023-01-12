/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.linkis.ecm.server.util;

import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.common.utils.Utils;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

import java.util.*;
import java.util.concurrent.*;

import scala.concurrent.ExecutionContextExecutorService;

import org.slf4j.Logger;

public class ThreadUtils extends ApplicationContextEvent {

    private static final Logger logger = LoggerFactory.getLogger(ThreadUtils.class);
    public static final String shellPath = Configuration.getLinkisHome() + "/admin/";

    public static ExecutionContextExecutorService executors =
            Utils.newCachedExecutionContext(5, "clean-ec-pool-thred", false);

    public ThreadUtils(ApplicationContext source) {
        super(source);
    }

    public static String run(List<String> cmdList, String shellName) {
        FutureTask future = new FutureTask(() -> Utils.exec(cmdList.toArray(new String[2]), -1));
        executors.submit(future);
        String msg = "";
        try {
            msg = future.get(30, TimeUnit.MINUTES).toString();
        } catch (TimeoutException e) {
            logger.info("shell执行超时 {}", shellName);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Thread error msg {}", e.getMessage());
        }
        return msg;
    }
}
