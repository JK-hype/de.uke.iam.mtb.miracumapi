package de.uke.iam.mtb.miracumapi.util;

import de.uke.iam.mtb.dto.miracum.MiracumInputDetailsDto;

public class MiracumPathBuilder {

    private final String pathToMIRACUM;

    public MiracumPathBuilder(String pathToMIRACUM) {
        this.pathToMIRACUM = pathToMIRACUM;
    }

    public String buildFilePathToPatientOutputFile(
            MiracumInputDetailsDto inputDetails,
            OutputFile file) {
        String path = buildFilePathToPatientOutput(inputDetails);
        String protocolWithPatientName = buildProtocolWithPatientName(inputDetails);

        path += "/Analyses";
        path += protocolWithPatientName + file.fileExtension;

        return path;
    }

    public String buildFilePathToPatientOutput(MiracumInputDetailsDto inputDetails) {
        String path = pathToMIRACUM;
        String protocolWithPatientName = buildProtocolWithPatientName(inputDetails);

        path += "assets/output";
        path += protocolWithPatientName;

        return path;
    }

    public String buildFilePathToPatientInput(String patientName) {
        if (patientName.isEmpty()) {
            return "";
        }
        return pathToMIRACUM + "assets/input/" + patientName;
    }

    public String buildFilePathToLogs(MiracumInputDetailsDto inputDetails) {

        String path = pathToMIRACUM;
        String protocolWithPatientName = buildProtocolWithPatientName(inputDetails);

        path += "assets/output";
        path += protocolWithPatientName + "/log";

        return path;
    }

    private String buildProtocolWithPatientName(MiracumInputDetailsDto inputDetails) {

        String protocolWithPatientName = "";

        switch (inputDetails.getProtocol()) {
            case WES:
                if (inputDetails.getAnnotationOfGermlineFindings()) {
                    protocolWithPatientName = "/somaticGermline_";
                } else {
                    protocolWithPatientName = "/somatic_";
                }
                break;
            case PANEL:
                protocolWithPatientName = "/panelTumor_";
                break;
            case TUMOR_ONLY:
                protocolWithPatientName += "/tumorOnly_";
                break;
        }

        protocolWithPatientName += inputDetails.getPatientName();

        return protocolWithPatientName;
    }

    public enum OutputFile {
        REPORT("_Report.pdf"), MAF(".maf");

        private String fileExtension;

        private OutputFile(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }
}
