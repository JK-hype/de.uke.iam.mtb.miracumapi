package de.uke.iam.mtb.miracumapi.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import de.uke.iam.mtb.dto.miracum.MiracumMafDto;

public class MiracumMafDtoDeserializer extends StdDeserializer<MiracumMafDto> {

    public MiracumMafDtoDeserializer() {
        this(null);
    }

    protected MiracumMafDtoDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MiracumMafDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        MiracumMafDto mafDto = new MiracumMafDto();
        JsonNode jsonNode = p.getCodec().readTree(p);

        mafDto.setHugoSymbol(nullSafetyCheck(jsonNode.get("Hugo_Symbol")));
        mafDto.setEntrezGeneId(nullSafetyCheck(jsonNode.get("Entrez_Gene_Id")));
        mafDto.setCenter(nullSafetyCheck(jsonNode.get("Center")));
        mafDto.setTranscriptId(nullSafetyCheck(jsonNode.get("Transcript_Id")));
        mafDto.setVariantClassification(nullSafetyCheck(jsonNode.get("Variant_Classification")));
        mafDto.setChromosome(nullSafetyCheck(jsonNode.get("Chromosome")));
        mafDto.setVariantType(nullSafetyCheck(jsonNode.get("Variant_Type")));
        mafDto.setStartPosition(nullSafetyCheck(jsonNode.get("Start_Position")));
        mafDto.setEndPosition(nullSafetyCheck(jsonNode.get("End_Position")));
        mafDto.setNcbiBuild(nullSafetyCheck(jsonNode.get("NCBI_Build")));
        mafDto.setReferenceAllele(nullSafetyCheck(jsonNode.get("Reference_Allele")));
        mafDto.setTumorSeqAllele1(nullSafetyCheck(jsonNode.get("Tumor_Seq_Allele1")));
        mafDto.setTumorSeqAllele2(nullSafetyCheck(jsonNode.get("Tumor_Seq_Allele2")));
        mafDto.setStrand(nullSafetyCheck(jsonNode.get("Strand")));
        mafDto.setDbsnpRs(nullSafetyCheck(jsonNode.get("dbSNP_RS")));
        mafDto.setDbsnpRsValStatus(nullSafetyCheck(jsonNode.get("dbSNP_Val_Status")));
        mafDto.setTumorSampleBarcode(nullSafetyCheck(jsonNode.get("Tumor_Sample_Barcode")));
        mafDto.setMatchedNormSampleBarcode(nullSafetyCheck(jsonNode.get("Matched_Norm_Sample_Barcode")));
        mafDto.setMatchedNormSeqAllele1(nullSafetyCheck(jsonNode.get("Match_Norm_Seq_Allele1")));
        mafDto.setMatchedNormSeqAllele2(nullSafetyCheck(jsonNode.get("Match_Norm_Seq_Allele2")));
        mafDto.setTumorValidationAllele1(nullSafetyCheck(jsonNode.get("Tumor_Validation_Allele1")));
        mafDto.setTumorValidationAllele2(nullSafetyCheck(jsonNode.get("Tumor_Validation_Allele2")));
        mafDto.setMatchedNormalValidationAllele1(nullSafetyCheck(jsonNode.get("Match_Norm_Validation_Allele1")));
        mafDto.setMatchedNormalValidationAllele2(nullSafetyCheck(jsonNode.get("Match_Norm_Validation_Allele2")));
        mafDto.setVerificationStatus(nullSafetyCheck(jsonNode.get("Verification_Status")));
        mafDto.setValidationStatus(nullSafetyCheck(jsonNode.get("Validation_Status")));
        mafDto.setMutationStatus(nullSafetyCheck(jsonNode.get("Mutation_Status")));
        mafDto.setSequencingPhase(nullSafetyCheck(jsonNode.get("Sequencing_Phase")));
        mafDto.setSequencingSource(nullSafetyCheck(jsonNode.get("Sequencing_Source")));
        mafDto.setValidationMethod(nullSafetyCheck(jsonNode.get("Validation_Method")));
        mafDto.setScore(nullSafetyCheck(jsonNode.get("Score")));
        mafDto.setBamFile(nullSafetyCheck(jsonNode.get("BAM_File")));
        mafDto.setSequencer(nullSafetyCheck(jsonNode.get("Sequencer")));
        mafDto.setHgvspShort(nullSafetyCheck(jsonNode.get("HGVSp_Short")));
        mafDto.setAminoAcidChange(nullSafetyCheck(jsonNode.get("Amino_Acid_Change")));
        mafDto.setTxChange(nullSafetyCheck(jsonNode.get("TxChange")));
        mafDto.setEnsemblGeneId(nullSafetyCheck(jsonNode.get("ENSEMBL_Gene_Id")));
        mafDto.setRefCountT(nullSafetyCheck(jsonNode.get("t_ref_count")));
        mafDto.setAltCountT(nullSafetyCheck(jsonNode.get("t_alt_count")));
        mafDto.setRefCountN(nullSafetyCheck(jsonNode.get("n_ref_count")));
        mafDto.setAltCountN(nullSafetyCheck(jsonNode.get("n_alt_count")));

        return mafDto;
    }

    private String nullSafetyCheck(JsonNode jsonNode) {
        return jsonNode == null ? null : jsonNode.asText();
    }
}
