package org.hkijena.jipipe.api.instrumentation;

public final class InstrumentationProtocol {

    private InstrumentationProtocol() {
    }

    public static final String EVENT_PROJECT_LIST = "project_list";
    public static final String EVENT_PROJECT_CHANGED = "project_changed";
    public static final String EVENT_OPERATION_RESULT = "operation_result";
    public static final String EVENT_OPERATION_LIST = "operation_list";
    public static final String EVENT_NODE_ADDED = "node_added";
    public static final String EVENT_NODE_REMOVED = "node_removed";
    public static final String EVENT_CONNECTION_CHANGED = "connection_changed";
    public static final String EVENT_PARAMETER_CHANGED = "parameter_changed";
    public static final String EVENT_COMPARTMENT_CHANGED = "compartment_changed";
    public static final String EVENT_JOB_STARTED = "job_started";
    public static final String EVENT_JOB_PROGRESS = "job_progress";
    public static final String EVENT_JOB_COMPLETED = "job_completed";
    public static final String EVENT_ERROR = "error";

    public static final String CMD_SELECT_PROJECT = "select_project";
    public static final String CMD_GET_JOB_STATUS = "get_job_status";
    public static final String CMD_CANCEL_JOB = "cancel_job";
}
