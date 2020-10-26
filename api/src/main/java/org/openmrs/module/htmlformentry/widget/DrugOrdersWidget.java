package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.util.JsonObject;

public class DrugOrdersWidget implements Widget {
	
	private static Log log = LogFactory.getLog(DrugOrdersWidget.class);
	
	private DrugOrderField drugOrderField;
	
	private DrugOrderWidgetConfig widgetConfig;
	
	private Map<Drug, DrugOrderWidget> drugOrderWidgets;
	
	private Map<Drug, List<DrugOrder>> initialValue;
	
	public DrugOrdersWidget(FormEntryContext context, DrugOrderField drugOrderField, DrugOrderWidgetConfig widgetConfig) {
		
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrdersWidget.constructor");
		
		this.drugOrderField = drugOrderField;
		this.widgetConfig = widgetConfig;
		
		for (DrugOrderAnswer answer : drugOrderField.getDrugOrderAnswers()) {
			DrugOrderWidget drugOrderWidget = new DrugOrderWidget(context, answer, widgetConfig);
			getDrugOrderWidgets().put(answer.getDrug(), drugOrderWidget);
		}
		sw.stop();
		log.trace("DrugOrdersWidget.constructor: " + sw.toString());
	}
	
	@Override
	public void setInitialValue(Object v) {
		initialValue = (Map<Drug, List<DrugOrder>>) v;
		if (initialValue != null) {
			for (Drug drug : initialValue.keySet()) {
				List<DrugOrder> drugOrders = initialValue.get(drug);
				HtmlFormEntryUtil.sortDrugOrders(drugOrders);
				if (drugOrders != null && drugOrders.size() > 0) {
					DrugOrderWidget w = getDrugOrderWidgets().get(drug);
					if (w != null) {
						w.setInitialValue(drugOrders.get(drugOrders.size() - 1));
					}
				}
			}
		}
	}
	
	public List<DrugOrder> getInitialValueForDrug(Drug drug) {
		return initialValue == null ? new ArrayList<>() : initialValue.getOrDefault(drug, new ArrayList<>());
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrdersWidget.generateHtml");
		
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a div
		startTag(writer, "div", fieldName, "drugorders-element", null);
		writer.println();
		
		// Add a section prior to the various drug widgets
		writer.println("<div id=\"" + fieldName + "_header\" class=\"drugorders-header-section\"></div>");
		
		// Establish a json config that initializes the javascript-based widget
		Integer patId = context.getExistingPatient().getPatientId();
		Integer encId = context.getExistingEncounter() == null ? null : context.getExistingEncounter().getEncounterId();
		
		JsonObject jsonConfig = new JsonObject();
		jsonConfig.addString("fieldName", fieldName);
		jsonConfig.addString("today", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		jsonConfig.addString("patientId", patId.toString());
		jsonConfig.addString("encounterId", encId == null ? "" : encId.toString());
		jsonConfig.addString("mode", context.getMode().name());
		if (widgetConfig.getAttributes() != null) {
			for (String att : widgetConfig.getAttributes().keySet()) {
				jsonConfig.addString(att, widgetConfig.getAttributes().get(att));
			}
		}
		
		// Add any translations needed by the default views
		String prefix = "htmlformentry.drugOrder.";
		JsonObject translations = jsonConfig.addObject("translations");
		translations.addTranslation(prefix, "asNeeded");
		translations.addTranslation(prefix, "previousOrder");
		translations.addTranslation(prefix, "present");
		translations.addTranslation(prefix, "noOrders");
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		for (Drug drug : getDrugOrderWidgets().keySet()) {
			DrugOrderWidget drugOrderWidget = getDrugOrderWidgets().get(drug);
			String drugLabel = drugOrderWidget.getDrugOrderAnswer().getDisplayName();
			
			// All elements for a given drug will have an id prefix like "fieldName_drugId"
			String drugOrderSectionId = fieldName + "_" + drug.getId();
			String sectionStyle = (drugOrderWidget.getInitialValue() == null ? "display:none" : "");
			startTag(writer, "div", drugOrderSectionId, "drugorders-drug-section", sectionStyle);
			writer.append("<div class=\"drugorders-drug-details\">").append("</div>");
			writer.append("<div class=\"drugorders-order-history\"></div>");
			String entryId = drugOrderSectionId + "_entry";
			startTag(writer, "div", entryId, "drugorders-order-form", "display:none;");
			writer.print(drugOrderWidget.generateHtml(context));
			writer.println();
			writer.println("</div>");
			
			writer.println("</div>");
			
			// For each rendered drugOrderWidget, add configuration of that widget into json for javascript
			JsonObject jsonDrug = jsonConfig.addObjectToArray("drugs");
			jsonDrug.addString("drugId", drug.getId().toString());
			jsonDrug.addString("drugLabel", drugLabel);
			jsonDrug.addString("sectionId", drugOrderSectionId);
			
			JsonObject jsonDrugWidgets = jsonDrug.addObject("widgets");
			for (String key : drugOrderWidget.getWidgetReplacements().keySet()) {
				jsonDrugWidgets.addString(key, context.getFieldName(drugOrderWidget.getWidgetReplacements().get(key)));
			}
			
			for (DrugOrder d : getInitialValueForDrug(drug)) {
				Order pd = d.getPreviousOrder();
				JsonObject jho = jsonDrug.addObjectToArray("history");
				jho.addString("orderId", d.getOrderId().toString());
				jho.addString("encounterId", d.getEncounter().getEncounterId().toString());
				jho.addString("previousOrderId", pd == null ? "" : pd.getOrderId().toString());
				jho.addIdAndLabel("action", "value", "display", d.getAction());
				jho.addIdAndLabel("drug", "value", "display", d.getDrug());
				jho.addIdAndLabel("careSetting", "value", "display", d.getCareSetting());
				jho.addIdAndLabel("dosingType", "value", "display", d.getDosingType());
				jho.addIdAndLabel("orderType", "value", "display", d.getOrderType());
				jho.addIdAndLabel("dosingInstructions", "value", "display", d.getDosingInstructions());
				jho.addIdAndLabel("dose", "value", "display", d.getDose());
				jho.addIdAndLabel("doseUnits", "value", "display", d.getDoseUnits());
				jho.addIdAndLabel("route", "value", "display", d.getRoute());
				jho.addIdAndLabel("frequency", "value", "display", d.getFrequency());
				jho.addIdAndLabel("asNeeded", "value", "display", d.getAsNeeded());
				jho.addIdAndLabel("instructions", "value", "display", d.getInstructions());
				jho.addIdAndLabel("urgency", "value", "display", d.getUrgency());
				jho.addIdAndLabel("dateActivated", "value", "display", d.getDateActivated());
				jho.addIdAndLabel("scheduledDate", "value", "display", d.getScheduledDate());
				jho.addIdAndLabel("effectiveStartDate", "value", "display", d.getEffectiveStartDate());
				jho.addIdAndLabel("duration", "value", "display", d.getDuration());
				jho.addIdAndLabel("durationUnits", "value", "display", d.getDurationUnits());
				jho.addIdAndLabel("autoExpireDate", "value", "display", d.getAutoExpireDate());
				jho.addIdAndLabel("dateStopped", "value", "display", d.getDateStopped());
				jho.addIdAndLabel("effectiveStopDate", "value", "display", d.getEffectiveStopDate());
				jho.addIdAndLabel("quantity", "value", "display", d.getQuantity());
				jho.addIdAndLabel("quantityUnits", "value", "display", d.getQuantityUnits());
				jho.addIdAndLabel("numRefills", "value", "display", d.getNumRefills());
				jho.addIdAndLabel("orderReason", "value", "display", d.getOrderReason());
			}
		}
		
		// Add javascript function to initialize widget as appropriate
		String defaultLoadFn = "htmlForm.initializeDrugOrdersWidgets";
		String onLoadFn = widgetConfig.getAttributes().getOrDefault("onLoadFunction", defaultLoadFn);
		writer.println("<script type=\"text/javascript\">");
		writer.println("jQuery(function() { " + onLoadFn + "(");
		writer.println(jsonConfig.toJson());
		writer.println(")});");
		writer.println("</script>");
		
		writer.println("</div>");
		
		sw.stop();
		log.trace("DrugOrdersWidget.generateHtml: " + sw.toString());
		
		return writer.getContent();
	}
	
	protected void startTag(CapturingPrintWriter w, String tagName, String elementId, String className, String cssStyle) {
		w.print("<" + tagName);
		w.print(" id=\"" + elementId + "\"");
		w.print(" class=\"" + className + "\"");
		if (StringUtils.isNotBlank(cssStyle)) {
			w.print(" style=\"" + cssStyle + "\"");
		}
		w.print(">");
	}
	
	@Override
	public List<DrugOrderWidgetValue> getValue(FormEntryContext context, HttpServletRequest request) {
		List<DrugOrderWidgetValue> ret = new ArrayList<>();
		for (DrugOrderWidget widget : getDrugOrderWidgets().values()) {
			DrugOrderWidgetValue drugOrder = widget.getValue(context, request);
			ret.add(drugOrder);
		}
		return ret;
	}
	
	public DrugOrderField getDrugOrderField() {
		return drugOrderField;
	}
	
	public void setDrugOrderField(DrugOrderField drugOrderField) {
		this.drugOrderField = drugOrderField;
	}
	
	public DrugOrderWidgetConfig getWidgetConfig() {
		return widgetConfig;
	}
	
	public void setWidgetConfig(DrugOrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
	}
	
	public Map<Drug, DrugOrderWidget> getDrugOrderWidgets() {
		if (drugOrderWidgets == null) {
			drugOrderWidgets = new LinkedHashMap<>();
		}
		return drugOrderWidgets;
	}
}
