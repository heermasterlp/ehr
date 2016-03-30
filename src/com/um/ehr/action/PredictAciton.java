package com.um.ehr.action;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.opensymphony.xwork2.ActionSupport;
import com.um.data.DiagClassifyData;
import com.um.model.ChineseMedicine;
import com.um.model.EHealthRecord;
import com.um.util.BasedOnRulePredict;
import com.um.util.DiagMedicineProcess;
import com.um.util.MachineLearningPredict;
import com.um.util.MedicineByDescription;

public class PredictAciton extends ActionSupport implements ServletRequestAware{
	
	private static Logger logger = Logger.getLogger("com.um.ehr.action.PredictAciton"); 
	
	 private static final long serialVersionUID = 1L;
     
	 private HttpServletRequest request;
	 
	 private String result;
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		// TODO Auto-generated method stub
		this.request = request;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
	
	/**
     * Predict medicines
     * @return SUCCESS
     */
    public String predictByInput(){
        try {
            //获取数据
            
            //将数据存储在map里，再转换成json类型数据，也可以自己手动构造json类型数据
            
            
            /**
             * 1. Parse request parameters
             */
        	// get parameters
        	String batch = request.getParameter("batch");
            double threshold = Double.valueOf(request.getParameter("threshold"));
            String timestatus = request.getParameter("timestatus");
            String xu = request.getParameter("xu");
            String tanyu = request.getParameter("tanyu");
            String tanshi = request.getParameter("tanshi");
            String zhengxing = request.getParameter("zhengxing");
            String sputumamount = request.getParameter("sputumamount");
            String sputumcolor = request.getParameter("sputumcolor");
            String cough = request.getParameter("cough");
            String pulse = request.getParameter("pulse");
            String na = request.getParameter("na");
            String defecate = request.getParameter("defecate");
            String constipation = request.getParameter("constipation");
            String urinate = request.getParameter("urinate");
            String xonglei = request.getParameter("xonglei");
            String futong = request.getParameter("futong");
            String tengtong = request.getParameter("tengtong");
            String bodydiscomfort = request.getParameter("bodydiscomfort");
            String tonguecolor = request.getParameter("tonguecolor");
            String coatedtongue = request.getParameter("coatedtongue");
            String energy = request.getParameter("energy");
            String sleep = request.getParameter("sleep");
            String hanre = request.getParameter("hanre");
            String sweat = request.getParameter("sweat");
            String thirst = request.getParameter("thirst");
            String taste = request.getParameter("taste");
            
            logger.info("batch: " + batch);
        	// format diagnose and description
            String diagnose = "";
            String description = "";
            
            // diagnose
            diagnose = zhengxing + (tanyu.equals("yes") ? "痰瘀," : "") + (tanshi.equals("yes") ? "痰湿,":"") + xu;
            
            // description
            description = timestatus + "," +sputumamount + "," + sputumcolor + "," + cough + "," + pulse + na + "," 
            				+ defecate + "," + constipation + "," + urinate + "," + xonglei + ","
            				+ futong + "," + tengtong + bodydiscomfort + tonguecolor + "," 
            				+ coatedtongue + "," + energy + "," + sleep + "," + hanre + ","
            				+ sweat + "," + thirst + "," + taste;
            // 1.3 formatted the description to output
    		String descconvertString = MedicineByDescription.getFormatedDescirption(description);
    		String descriptionString = diagnose + descconvertString;
            
            /**
             * 2. Predict medicines based on statistics method
             */
            List<String> medicineListByStatis = new ArrayList<String>(); // predict medicines result
            // 2.1 statistics medicines larger than 90% records
    		int outputnumber = 15; // the number of output medicine
    		int similarnumber = 6; // similar record number
    		
    		// 2.2 get all records with same batch
    		List<EHealthRecord> eHealthRecordsByBatch = MedicineByDescription.getRecordsByBatch(batch); // all record with same batch
    		
    		// 2.3 statistics name and number of medicines in this batch records
    		Map<String, Integer> allMedicineMap = DiagMedicineProcess.statisEhealthMedicine(eHealthRecordsByBatch);
    		
    		// 2.4  find the medicines with percent larger than 90% 
    		int allRecordsNum = eHealthRecordsByBatch.size(); // the number of this batch records
    		double percent = 0.9; // the percent 
    		
    		List<String> medicineWithInevitable = DiagMedicineProcess.statisMedicineWithPercent(allMedicineMap, allRecordsNum, percent);
    		if(medicineWithInevitable != null && medicineWithInevitable.size() > 0){
    			medicineListByStatis.addAll(medicineWithInevitable); //the medicine with percent large than 90%
    		}
    		
    		// 2.6 get similar records based on the description
    		List<EHealthRecord> similaryRecords = MedicineByDescription.getSimilaryEHealthRecords(eHealthRecordsByBatch, diagnose, description);
    		
    		if (similaryRecords != null && similaryRecords.size() > 0) {
    			// 2.7 statistic the medicines in the similar records
    			Set<String> cnmedicineSet = DiagMedicineProcess.getMedicinesByDescription(description, similaryRecords);
    			for (String med : medicineListByStatis) {
    				if (!cnmedicineSet.contains(med)) {
    					// remove the medicine from medicine list not in the cnmedicine set
    					medicineListByStatis.remove(med);
    				}
    			}
    			for (String cn : cnmedicineSet) {
    				if (!medicineListByStatis.contains(cn)) {
    					// add to result list
    					medicineListByStatis.add(cn);
    				}
    			}
    		}
    		
    		if (medicineListByStatis.size() > outputnumber) {
    			medicineListByStatis = medicineListByStatis.subList(0, outputnumber);
    		}
    		
    		// 2.7 Sort the medicine with same order with machine learning result
    		List<String> medicineListByStatisticSorted = new ArrayList<String>();
    		for( String s : DiagClassifyData.machineMedicine ){
    			if (medicineListByStatis.contains(s)) {
    				medicineListByStatisticSorted.add(s);
    			}
    		}
            
            
            /**
             * 3. Predict medicines based on machine learning method
             */
    		//  3.1 initial the input parameters of machine learning
    		List<String> inputcode = MachineLearningPredict.parseDiagAndDesc(diagnose, description); // format the input parameters
    		// 	3.2 predict the medicines based on the machine learning
    		List<String> medicineListByMachine = MachineLearningPredict.predict(inputcode, threshold); // the predict result of machine learning
    		
            /**
             * 4. Predict medicines based on rules
             */
    		List<String> medicineListByRules = BasedOnRulePredict.predictBasedOnRules(descriptionString);
            /**
             * 5. Return result
             */
    		List<String> medicineList = new ArrayList<>(); // compensive result with statistics, machine learning and rules
    		
    		Set<String> union = new HashSet<>();
    		union.addAll(medicineListByStatisticSorted);
    		union.addAll(medicineListByMachine);
    		union.addAll(medicineListByRules);
    		
    		Map<String, String> uninomap = new HashMap<>();
    		// check medicines and count in those list
    		for (String un : union) {
				if (medicineListByStatisticSorted.contains(un) && medicineListByMachine.contains(un) && medicineListByRules.contains(un)) {
					// all in those list
					uninomap.put(un, "3");
				}
				if (!medicineListByStatisticSorted.contains(un) && medicineListByMachine.contains(un) && medicineListByRules.contains(un) ||
						medicineListByStatisticSorted.contains(un) && !medicineListByMachine.contains(un) && medicineListByRules.contains(un) ||
						medicineListByStatisticSorted.contains(un) && medicineListByMachine.contains(un) && !medicineListByRules.contains(un)) {
					// in 2 list
					uninomap.put(un, "2");
				}
				if (!medicineListByStatisticSorted.contains(un) && !medicineListByMachine.contains(un) && medicineListByRules.contains(un) ||
						!medicineListByStatisticSorted.contains(un) && medicineListByMachine.contains(un) && !medicineListByRules.contains(un) ||
						medicineListByStatisticSorted.contains(un) && !medicineListByMachine.contains(un) && !medicineListByRules.contains(un)) {
					// only in 1 list
					uninomap.put(un, "1");
				}
				
			}
    		// result not enough, add medicines in 2 list
    		Set<String> unionKeyset = uninomap.keySet();
    		// add medicines in 3 list
    		for (String un : unionKeyset) {
				if (uninomap.get(un).equals("3")) {
					medicineList.add(un);
				}
			}
    		
    		// add medicines in 2 list
    		if (medicineList.size() < outputnumber) {
    			for (String un : unionKeyset) {
    				if (uninomap.get(un).equals("2") && medicineList.size() < outputnumber) {
    					medicineList.add(un);
    				}
    			}
			}
    		
    		// add medicines only in 1 list
    		if (medicineList.size() < outputnumber) {
    			for (String un : unionKeyset) {
    				if (uninomap.get(un).equals("1") && medicineList.size() < outputnumber) {
    					medicineList.add(un);
    				}
    			}
			}
    		
    		
            Map<String,Object> map = new HashMap<String,Object>();
            
//            map.put("medicineListByStatistics", medicineListByStatisticSorted);
//            map.put("medicineListByRules", medicineListByRules);
//            map.put("medicineListByMachine", medicineListByMachine);
            
            map.put("medicineList", medicineList);
            
            JSONObject json = JSONObject.fromObject(map);//将map对象转换成json类型数据
            result = json.toString();//给result赋值，传递给页面
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SUCCESS;
    }
    
    /**
     *  Predict by case
     * @return
     */
    public String predictByCase(){
    	
    	logger.info("predict by case start!");
    	
    	// 1. get the input parameters
    	String countString = request.getParameter("count"); // the order number of records
    			
    	int count = 0; // record order number
    	double threshold = Double.valueOf(request.getParameter("threshold").trim()); // threshold of machine learning
    	// 2. find all records with batch 2012
    	List<EHealthRecord> allList = MedicineByDescription.getRecordsByBatch("2012");
    	if ("".equals(countString)) {
    		return SUCCESS;
    	}
    	// 3. find the target record based on the conditions
    	EHealthRecord targetRecord = null;
    			
    	if( countString.length() > 4 ){
    		// the input info is the register number of record
    		for( EHealthRecord e : allList ){
    			if( e.getRegistrationno().equals(countString) ){
    				targetRecord = e;
    				break;
    			}
    			count++;
    		}
    	}else{
    		// the input info is the order number of all records
    		count = Integer.valueOf(countString); // order number
    		count--;
    		targetRecord = allList.get( count );
    				
    	}
    			
    	if(targetRecord == null){
    		return SUCCESS;
    	}
    	//4. the diagnose and description info of target record
    	String diag = targetRecord.getChinesediagnostics();
    	String description = targetRecord.getConditionsdescribed();
    	String diagnose = "";
    	String[] diagKeywords = DiagClassifyData.diagKeywords;
    	for( String k : diagKeywords ){
    		if(diag.matches(".*" + k + ".*")){
    			diagnose += k + " ";
    		}
    	}
    	// format the description of target record
    	String formattedDescription = MedicineByDescription.formattedDescriptionByCount(description);
    	// 5. the origin medicines in target record            
    	List<String> orignMedicines = new ArrayList<String>();
    	if( targetRecord.getChineseMedicines() != null && targetRecord.getChineseMedicines().size() > 0 ){
    		for(ChineseMedicine c : targetRecord.getChineseMedicines()){
    			orignMedicines.add(c.getNameString());
    		}
    	}
    			
    	// 6. sort the origin medicines with a fix order
    	List<String> sortedList = new ArrayList<String>();
    			
    	for( String s : DiagClassifyData.machineMedicine ){
    		for( String o : orignMedicines ){
    			if( s == o || s.equals(o) ){
    				sortedList.add(s);
    			}
    		}
    	}
    			
    	// 7. predict medicines with machine learning 
    	//  7.1 initial input parameters of machine learning
    	List<String> inputcode = MachineLearningPredict.parseDiagAndDescByEhealthRecords(targetRecord);
    			
    	//  7.2 predict medicines with machine learning
    	List<String> medicineListByMachine = MachineLearningPredict.predict(inputcode, threshold); // the result of machine learning
    			
    	// 8. calculate the accuracy
    	double statisticsPercent = 0.0; // the accuracy of case-based
    	double mechineLearningPercent = 0.0;  // the accuracy of machine learning
    			
    	int index = 0;
    			
    	statisticsPercent = 100.0 * orignMedicines.size() / orignMedicines.size(); //accuracy of case-based
    	index = 0;
    			
    	for( String s : medicineListByMachine ){
    		if( orignMedicines.contains(s) ){
    			index++;
    		}
    	}
    	mechineLearningPercent = 100.0 * index / orignMedicines.size(); // the accuracy of machine learning
    	DecimalFormat df = new DecimalFormat("#.##");
    	
    	// 9. format result
    	Map<String, Object> map = new HashMap<String, Object>();
    	map.put("orignMedicines", orignMedicines);
    	map.put("medicineListByMachine", medicineListByMachine);
    	map.put("statisticsPercent", df.format(statisticsPercent));
    	map.put("mechineLearningPercent", df.format(mechineLearningPercent));
    	
    	JSONObject json = JSONObject.fromObject(map);
    	result = json.toString();
    	
    	logger.info("predict by case end!");
    	
    	return SUCCESS;
    }
	
	
}
