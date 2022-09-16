package de.uke.iam.mtb.miracumapi.util;

import static de.uke.iam.mtb.miracumapi.util.MiracumConverter.convertJobDataMapToYamlConf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uke.iam.mtb.miracumapi.service.MiracumService;

@Component
public class MiracumQuartzJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiracumQuartzJob.class);
    @Autowired
    private MiracumService miracumService;

    public MiracumQuartzJob() {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ProcessBuilder processBuilder = new ProcessBuilder("docker", "ps");
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String patientName = dataMap.getString("patientName");
        try {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            if (lines.stream().anyMatch(l -> l.contains(patientName))) {
                LOGGER.info("Pipeline for " + patientName + " is still running");
            } else {
                LOGGER.info("Pipeline for " + patientName + " has stopped");
                miracumService.sendMafFile(convertJobDataMapToYamlConf(dataMap));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}