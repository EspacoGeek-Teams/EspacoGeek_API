package com.espacogeek.geek.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchJobExecution {
    private String id;
    private String jobName;
    private String status;
    private String startTime;
    private String endTime;
    private String exitCode;
}
