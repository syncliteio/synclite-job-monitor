/*
 * Copyright (c) 2024 mahendra.chavan@synclite.io, all rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.synclite.jobmonitor.web;

import java.nio.file.Path;

import org.json.JSONObject;

public class JobSchedule {
	public Long scheduleID;
    public String jobName;
    public String jobType;
    public Integer scheduleStartHour;
    public Integer scheduleStartMinute;
    public Integer scheduleEndHour;
    public Integer scheduleEndMinute;
    public Integer jobRunInterval;
    public String jobRunIntervalUnit;
    public Integer jobRunDuration;
    public String jobRunDurationUnit;
	public String jobSubType;
	
	public JobSchedule(JSONObject o) {
		this.scheduleID = o.getLong("scheduleID");
		this.jobName = o.getString("jobName");
		this.jobType = o.getString("jobType");
		this.scheduleStartHour = o.getInt("scheduleStartHour");
		this.scheduleStartMinute = o.getInt("scheduleStartMinute");
		this.scheduleEndHour = o.getInt("scheduleEndHour");
		this.scheduleEndMinute = o.getInt("scheduleEndMinute");
		this.jobRunInterval = o.getInt("jobRunInterval");
		this.jobRunIntervalUnit = o.getString("jobRunIntervalUnit");
		this.jobRunDuration = o.getInt("jobRunDuration");
		this.jobRunDurationUnit = o.getString("jobRunDurationUnit");
		this.jobSubType = o.getString("jobSubType");	
	}
	
	public JobSchedule() {
	}

	public JSONObject getJSONObject() {
		JSONObject o = new JSONObject();
		o.put("scheduleID", scheduleID);
		o.put("jobName", jobName);
		o.put("jobType", jobType);
		o.put("scheduleStartHour", scheduleStartHour);
		o.put("scheduleStartMinute", scheduleStartMinute);
		o.put("scheduleEndHour", scheduleEndHour);
		o.put("scheduleEndMinute", scheduleEndMinute);
		o.put("jobRunInterval", jobRunInterval);
		o.put("jobRunIntervalUnit", jobRunIntervalUnit);
		o.put("jobRunDuration", jobRunDuration);
		o.put("jobRunDurationUnit", jobRunDurationUnit);
		o.put("jobSubType", jobSubType);
		
		return o;
	}

}
