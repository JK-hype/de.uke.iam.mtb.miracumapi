package de.uke.iam.mtb.miracumapi.util;

import java.lang.reflect.Field;
import java.util.Map.Entry;

import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uke.iam.mtb.dto.miracum.MiracumInputDetailsDto;

public class MiracumConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiracumConverter.class);

    public static JobDataMap convertInputDetailsToJobDataMap(MiracumInputDetailsDto inputDetails) {
        JobDataMap jobDataMap = new JobDataMap();
        Field[] fields = inputDetails.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                jobDataMap.put(field.getName(), field.get(inputDetails));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                LOGGER.error("Failed to convert patient data of " + inputDetails.getPatientNameWithUnderscore()
                        + " to JobDataMap");
                e.printStackTrace();
                return new JobDataMap();
            }
        }

        jobDataMap.put("patientName", inputDetails.getPatientNameWithUnderscore());
        return jobDataMap;
    }

    public static MiracumInputDetailsDto convertJobDataMapToYamlConf(JobDataMap jobDataMap) {
        MiracumInputDetailsDto yamlConfDto = new MiracumInputDetailsDto();
        Field[] fields = yamlConfDto.getClass().getDeclaredFields();
        String patientName = jobDataMap.getString("patientName");

        for (Field field : fields) {
            field.setAccessible(true);
            for (Entry<String, Object> entry : jobDataMap.entrySet()) {
                if (field.getName().equals(entry.getKey())) {
                    try {
                        field.set(yamlConfDto, entry.getValue());
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        LOGGER.error("Failed to convert patient data of " + patientName + " to MiracumYamlConfDto");
                        e.printStackTrace();
                        return new MiracumInputDetailsDto();
                    }
                }
            }
        }
        return yamlConfDto;
    }

}
