/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.homeautomation.androidglass.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.homeautomation.androidglass.api.constants.AndroidGlassConstants;
import org.homeautomation.androidglass.api.util.APIUtil;
import org.homeautomation.androidglass.api.util.AndroidGlassConfiguration;
import org.homeautomation.androidglass.api.util.SensorRecord;
import org.json.JSONObject;
import org.wso2.carbon.analytics.dataservice.commons.SortByField;
import org.wso2.carbon.analytics.dataservice.commons.SortType;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.EnrolmentInfo;
import org.wso2.carbon.device.mgt.common.InvalidDeviceException;
import org.wso2.carbon.device.mgt.common.authorization.DeviceAccessAuthorizationException;
import org.wso2.carbon.device.mgt.common.configuration.mgt.ConfigurationManagementException;
import org.wso2.carbon.device.mgt.common.group.mgt.DeviceGroupConstants;
import org.wso2.carbon.device.mgt.common.operation.mgt.Operation;
import org.wso2.carbon.device.mgt.common.operation.mgt.OperationManagementException;
import org.wso2.carbon.device.mgt.core.operation.mgt.CommandOperation;


import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;


/**
 * This is the API which is used to control and manage device type functionality
 */
public class DeviceTypeServiceImpl implements DeviceTypeService {
    private static Log log = LogFactory.getLog(DeviceTypeService.class);


    @Path("device/stats/{deviceId}")
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    public Response getAndroidGlassDeviceStats(@PathParam("deviceId") String deviceId, @QueryParam("from") long from,
                                               @QueryParam("to") long to, @QueryParam("sensorType") String sensorType) {
        String fromDate = String.valueOf(from*1000); //converting to ms
        String toDate = String.valueOf(to*1000); // converting to ms
        String query = "meta_deviceId:" + deviceId + " AND meta_deviceType:" +
                AndroidGlassConstants.DEVICE_TYPE + " AND _timestamp : [" + fromDate + " TO " + toDate + "]";

        String sensorTableName = null;
        switch(sensorType){
            case AndroidGlassConstants.SENSOR_TYPE1:{sensorTableName= AndroidGlassConstants.SENSOR_TYPE1_EVENT_TABLE;break;}
            case AndroidGlassConstants.SENSOR_TYPE2:{sensorTableName= AndroidGlassConstants.SENSOR_TYPE2_EVENT_TABLE;break;}
            case AndroidGlassConstants.SENSOR_TYPE3:{sensorTableName= AndroidGlassConstants.SENSOR_TYPE3_EVENT_TABLE;break;}
            case AndroidGlassConstants.SENSOR_TYPE4:{sensorTableName= AndroidGlassConstants.SENSOR_TYPE4_EVENT_TABLE;break;}
            case AndroidGlassConstants.SENSOR_TYPE5:{sensorTableName= AndroidGlassConstants.SENSOR_TYPE5_EVENT_TABLE;break;}
        }
        try {
            if (!APIUtil.getDeviceAccessAuthorizationService().isUserAuthorized(
                    new DeviceIdentifier(deviceId, AndroidGlassConstants.DEVICE_TYPE),
                    DeviceGroupConstants.Permissions.DEFAULT_STATS_MONITOR_PERMISSIONS)) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
            List<SortByField> sortByFields = new ArrayList<>();
            SortByField sortByField = new SortByField("_timestamp", SortType.ASC);
            sortByFields.add(sortByField);
            List<SensorRecord> sensorRecords = APIUtil.getAllEventsForDevice(sensorTableName, query, sortByFields);
            return Response.status(Response.Status.OK.getStatusCode()).entity(sensorRecords).build();
        } catch (AnalyticsException e) {
            String errorMsg = "Error on retrieving stats on table " + sensorTableName + " with query " + query;
            log.error(errorMsg);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMsg).build();
        } catch (DeviceAccessAuthorizationException e) {
            log.error(e.getErrorMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Display message in Android Glass device.
     * @param deviceId : The registered device id.
     */
    @POST
    @Path("device/{deviceId}/message")
    @Override
    public Response sendMessage(@PathParam("deviceId") String deviceId, @QueryParam("message") String message) {
        try {
            if (!APIUtil.getDeviceAccessAuthorizationService().isUserAuthorized(new DeviceIdentifier(deviceId,
                    AndroidGlassConstants.DEVICE_TYPE), DeviceGroupConstants.Permissions.DEFAULT_OPERATOR_PERMISSIONS)) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
            String publishTopic = APIUtil.getAuthenticatedUserTenantDomain()
                    + "/" + AndroidGlassConstants.DEVICE_TYPE + "/" + deviceId + "/command";

            Operation commandOp = new CommandOperation();
            commandOp.setCode("message");
            commandOp.setType(Operation.Type.COMMAND);
            commandOp.setEnabled(true);

            JSONObject payload = new JSONObject();
            payload.put("action", commandOp.getCode());
            payload.put("payload", message);

            commandOp.setPayLoad(payload.toString());

            Properties props = new Properties();
            props.setProperty(AndroidGlassConstants.MQTT_ADAPTER_TOPIC_PROPERTY_NAME, publishTopic);
            commandOp.setProperties(props);

            List<DeviceIdentifier> deviceIdentifiers = new ArrayList<>();
            deviceIdentifiers.add(new DeviceIdentifier(deviceId, AndroidGlassConstants.DEVICE_TYPE));
            APIUtil.getDeviceManagementService().addOperation(AndroidGlassConstants.DEVICE_TYPE, commandOp,
                    deviceIdentifiers);
            return Response.ok().build();
        } catch (InvalidDeviceException e) {
            String msg = "Invalid Device Identifiers found.";
            log.error(msg, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (DeviceAccessAuthorizationException e) {
            log.error(e.getErrorMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
        } catch (OperationManagementException e) {
            log.error("Error occurred while executing command operation", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    @Path("device/{device_id}/register")
    @POST
    public Response register(@PathParam("device_id") String deviceId, @QueryParam("deviceName") String deviceName) {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId(deviceId);
        deviceIdentifier.setType(AndroidGlassConstants.DEVICE_TYPE);
        try {
            if (APIUtil.getDeviceManagementService().isEnrolled(deviceIdentifier)) {
                AndroidGlassConfiguration androidGlassConfiguration = new AndroidGlassConfiguration();
                androidGlassConfiguration.setTenantDomain(APIUtil.getAuthenticatedUserTenantDomain());
                androidGlassConfiguration.setMqttEndpoint(APIUtil.getMqttEndpoint());
                return Response.status(Response.Status.ACCEPTED.getStatusCode()).entity(androidGlassConfiguration.toString())
                        .build();
            }
            Device device = new Device();
            device.setDeviceIdentifier(deviceId);
            EnrolmentInfo enrolmentInfo = new EnrolmentInfo();
            enrolmentInfo.setDateOfEnrolment(new Date().getTime());
            enrolmentInfo.setDateOfLastUpdate(new Date().getTime());
            enrolmentInfo.setStatus(EnrolmentInfo.Status.ACTIVE);
            device.setName(deviceName);
            device.setType(AndroidGlassConstants.DEVICE_TYPE);
            enrolmentInfo.setOwner(APIUtil.getAuthenticatedUser());
            enrolmentInfo.setOwnership(EnrolmentInfo.OwnerShip.BYOD);
            device.setEnrolmentInfo(enrolmentInfo);
            boolean added = APIUtil.getDeviceManagementService().enrollDevice(device);
            if (added) {
                AndroidGlassConfiguration androidGlassConfiguration = new AndroidGlassConfiguration();
                androidGlassConfiguration.setTenantDomain(APIUtil.getAuthenticatedUserTenantDomain());
                androidGlassConfiguration.setMqttEndpoint(APIUtil.getMqttEndpoint());
                return Response.ok(androidGlassConfiguration.toString()).build();
            } else {
                return Response.status(Response.Status.NOT_ACCEPTABLE.getStatusCode()).entity(false).build();
            }
        } catch (DeviceManagementException | ConfigurationManagementException e) {
            log.error(e.getClass().getSimpleName(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(false).build();
        }
    }

}