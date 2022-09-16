package de.uke.iam.mtb.miracumapi.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import de.uke.iam.mtb.dto.miracum.MiracumInputDetailsDto;

public class MiracumInputDetailsToYamlSerializer extends StdSerializer<MiracumInputDetailsDto> {

    public MiracumInputDetailsToYamlSerializer() {
        this(null);
    }

    protected MiracumInputDetailsToYamlSerializer(Class<MiracumInputDetailsDto> t) {
        super(t);
    }

    @Override
    public void serialize(MiracumInputDetailsDto inputDetails, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("common");
        jsonGenerator.writeObjectFieldStart("files");
        jsonGenerator.writeObjectFieldStart("panel");
        jsonGenerator.writeStringField("tumor", inputDetails.getPatientNameWithUnderscore() + "_");
        jsonGenerator.writeStringField("numberOfFiles", Integer.toString(inputDetails.getNumberOfFilePairs()));
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
        jsonGenerator.writeStringField("protocol", inputDetails.getProtocol().toString());
        jsonGenerator.writeStringField("germline", inputDetails.getAnnotationOfGermlineFindings() ? "yes" : "no");
        jsonGenerator.writeEndObject();
        jsonGenerator.writeStringField("sex", inputDetails.getSex().getAnnotation());
        jsonGenerator.writeEndObject();
    }
}
