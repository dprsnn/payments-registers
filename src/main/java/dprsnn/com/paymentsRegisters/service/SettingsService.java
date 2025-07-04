package dprsnn.com.paymentsRegisters.service;

import dprsnn.com.paymentsRegisters.models.CrmCredentialsModel;
import dprsnn.com.paymentsRegisters.repos.CrmCredentialsRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {
    private final CrmCredentialsRepo crmCredentialsRepo;

    public SettingsService(CrmCredentialsRepo crmCredentialsRepo) {
        this.crmCredentialsRepo = crmCredentialsRepo;
    }

    public List<CrmCredentialsModel> loadSettingsPage(){
        return crmCredentialsRepo.findAll();
    }

    public void savePayments(String novaPay, String monoUht, String monoHh, String monoSwell, String evoPay, String ukrpost) {
        // NOVA_PAY
        if (novaPay != null && !novaPay.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("NOVA_PAY");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("NOVA_PAY");
            }

            crmCredentialsModel.setParamValue(novaPay);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        // MONO_UHT
        if (monoUht != null && !monoUht.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("MONO_UHT");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("MONO_UHT");
            }

            crmCredentialsModel.setParamValue(monoUht);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        // MONO_HH
        if (monoHh != null && !monoHh.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("MONO_HH");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("MONO_HH");
            }

            crmCredentialsModel.setParamValue(monoHh);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        // MONO_SWELL
        if (monoSwell != null && !monoSwell.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("MONO_SWELL");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("MONO_SWELL");
            }

            crmCredentialsModel.setParamValue(monoSwell);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        // EVO_PAY
        if (evoPay != null && !evoPay.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("EVO_PAY");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("EVO_PAY");
            }

            crmCredentialsModel.setParamValue(evoPay);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
        // UKRPOST
        if (ukrpost != null && !ukrpost.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("UKRPOST");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("UKRPOST");
            }

            crmCredentialsModel.setParamValue(ukrpost);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
    }

    public void saveOtherSettings(String novaExpenseId, String statusId, String customFieldId, String monoExpenseId, String evoExpenseId) {
        // EXPENSE_ID
        if (novaExpenseId != null && !novaExpenseId.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("NOVA_EXPENSE_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("NOVA_EXPENSE_ID");
            }

            crmCredentialsModel.setParamValue(novaExpenseId);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
        if (monoExpenseId != null && !monoExpenseId.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("MONO_EXPENSE_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("MONO_EXPENSE_ID");
            }

            crmCredentialsModel.setParamValue(monoExpenseId);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        // STATUS_ID
        if (statusId != null && !statusId.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("STATUS_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("STATUS_ID");
            }

            crmCredentialsModel.setParamValue(statusId);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        // CUSTOM_FIELD_ID
        if (customFieldId != null && !customFieldId.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("CUSTOM_FIELD_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("CUSTOM_FIELD_ID");
            }

            crmCredentialsModel.setParamValue(customFieldId);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
        // EVO_EXPENSE_ID
        if (evoExpenseId != null && !evoExpenseId.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("EVO_EXPENSE_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("EVO_EXPENSE_ID");
            }

            crmCredentialsModel.setParamValue(evoExpenseId);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
    }

    public void saveSources(String uhtSource, String swellSource, String hhSource) {

        if (uhtSource != null && !uhtSource.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("UHT_SOURCE_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("UHT_SOURCE_ID");
            }

            crmCredentialsModel.setParamValue(uhtSource);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        if (swellSource != null && !swellSource.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("SWELL_SOURCE_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("SWELL_SOURCE_ID");
            }

            crmCredentialsModel.setParamValue(swellSource);
            crmCredentialsRepo.save(crmCredentialsModel);
        }

        if (hhSource != null && !hhSource.isEmpty()) {
            CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("HH_SOURCE_ID");

            if (crmCredentialsModel == null) {
                crmCredentialsModel = new CrmCredentialsModel();
                crmCredentialsModel.setParamName("HH_SOURCE_ID");
            }

            crmCredentialsModel.setParamValue(hhSource);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
    }

    public String getApiKey(){
        CrmCredentialsModel record = crmCredentialsRepo.findByParamName("API_KEY");
        return record != null ? record.getParamValue() : "";
    }

    public String getStatusId(){
        return crmCredentialsRepo.findByParamName("STATUS_ID").getParamValue();
    }

    public String getCustomField(){
        return crmCredentialsRepo.findByParamName("CUSTOM_FIELD_ID").getParamValue();
    }

    public String getNovaExpenseId(){
        return  crmCredentialsRepo.findByParamName("NOVA_EXPENSE_ID").getParamValue();
    }

    public String getMonoExpenseId(){
        return  crmCredentialsRepo.findByParamName("MONO_EXPENSE_ID").getParamValue();
    }

    public int getNovaPayId(){
        return Integer.parseInt(crmCredentialsRepo.findByParamName("NOVA_PAY").getParamValue());
    }

    public int getMonoUhtId(){
        return Integer.parseInt(crmCredentialsRepo.findByParamName("MONO_UHT").getParamValue());
    }

    public int getMonoHHId(){
        return Integer.parseInt(crmCredentialsRepo.findByParamName("MONO_HH").getParamValue());
    }

    public int getMonoSwellId(){
        return Integer.parseInt(crmCredentialsRepo.findByParamName("MONO_SWELL").getParamValue());
    }

    public void setApiKey(String apiKey){
        CrmCredentialsModel crmCredentialsModel = crmCredentialsRepo.findByParamName("API_KEY");
//        System.out.println(crmCredentialsModel);

        if (crmCredentialsModel == null){
            CrmCredentialsModel credentialsModel = new CrmCredentialsModel("API_KEY", apiKey);
            crmCredentialsRepo.save(credentialsModel);
        } else {
            crmCredentialsModel.setParamValue(apiKey);
            crmCredentialsRepo.save(crmCredentialsModel);
        }
    }

    public int getUhtSourceId() {
        return Integer.parseInt(crmCredentialsRepo.findByParamName("UHT_SOURCE_ID").getParamValue());
    }

    public int getHHSourceId() {
        return Integer.parseInt(crmCredentialsRepo.findByParamName("HH_SOURCE_ID").getParamValue());
    }

    public int getSwellSourceId() {
        return Integer.parseInt(crmCredentialsRepo.findByParamName("SWELL_SOURCE_ID").getParamValue());
    }

    public int getEvoPayId() {
        return Integer.parseInt(crmCredentialsRepo.findByParamName("EVO_PAY").getParamValue());
    }

    public String getEvoExpenseId() {
        return  crmCredentialsRepo.findByParamName("EVO_EXPENSE_ID").getParamValue();
    }

    public int getUkrpostId() {
        return Integer.parseInt(crmCredentialsRepo.findByParamName("UKRPOST").getParamValue());
    }
}
