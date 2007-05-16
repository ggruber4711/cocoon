/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.cron;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

/**
 * QuartzJobScheduler that runs jobs within a Cocoon Environment.
 */
public class CocoonQuartzJobScheduler extends QuartzJobScheduler {

    protected JobDetail createJobDetail(String name, JobDataMap jobDataMap) {
        JobDetail detail = new JobDetail(name, DEFAULT_QUARTZ_JOB_GROUP, CocoonQuartzJobExecutor.class);
        detail.setJobDataMap(jobDataMap);
        return detail;
    }

    protected Job createJobExecutor() {
        return new CocoonQuartzJobExecutor();
    }

}
