package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;

/**
 * Holds the widgets used to represent a specific drug order
 */
public class DrugOrderWidget implements Widget {
	
	protected final Log log = LogFactory.getLog(DrugOrderWidget.class);
	
	private final DrugOrderAnswer drugOrderAnswer;
	
	private final String template;
	
	private final Map<String, Map<String, String>> widgetConfig;
	
	private final Map<String, Widget> widgetReplacements = new HashMap<>();
	
	private DrugOrder initialValue;
	
	private Widget drugWidget;
	
	private Widget actionWidget;
	
	private Widget careSettingWidget;
	
	private Widget dosingTypeWidget;
	
	private Widget orderTypeWidget;
	
	private Widget dosingInstructionsWidget;
	
	private Widget doseWidget;
	
	private Widget doseUnitsWidget;
	
	private Widget routeWidget;
	
	private Widget frequencyWidget;
	
	private Widget asNeededWidget;
	
	private Widget instructionsWidget;
	
	private Widget urgencyWidget;
	
	private Widget scheduledDateWidget;
	
	private Widget durationWidget;
	
	private Widget durationUnitsWidget;
	
	private Widget quantityWidget;
	
	private Widget quantityUnitsWidget;
	
	private Widget numRefillsWidget;
	
	public DrugOrderWidget(FormEntryContext context, DrugOrderAnswer drugOrderAnswer, String template,
	    Map<String, Map<String, String>> widgetConfig) {
		this.drugOrderAnswer = drugOrderAnswer;
		this.template = template;
		this.widgetConfig = widgetConfig;
		configureDrugWidget(context);
		configureActionWidget(context);
		configureCareSettingWidget(context);
		configureDosingTypeWidget(context);
		configureOrderTypeWidget(context);
		configureDosingInstructionsWidget(context);
		configureDoseWidget(context);
		configureDoseUnitsWidget(context);
		configureRouteWidget(context);
		configureFrequencyWidget(context);
		configureAsNeededWidget(context);
		configureInstructionsWidget(context);
		configureUrgencyWidget(context);
		configureScheduledDateWidget(context);
		configureDurationWidget(context);
		configureDurationUnitsWidget(context);
		configureQuantityWidget(context);
		configureQuantityUnitsWidget(context);
		configureNumRefillsWidget(context);
	}
	
	protected void configureDrugWidget(FormEntryContext context) {
		HiddenFieldWidget w = new HiddenFieldWidget();
		w.addAttribute("class", "order-property drug");
		w.setInitialValue(drugOrderAnswer.getDrug().getId().toString());
		w.setLabel(drugOrderAnswer.getDisplayName());
		drugWidget = w;
		registerWidget(context, w, new ErrorWidget(), "drug");
	}
	
	protected void configureActionWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("action", new HashMap<>());
		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option("", "", true));
		for (Order.Action a : Order.Action.values()) {
			w.addOption(new Option(a.name(), a.name(), false));
		}
		w.setInitialValue(config.get("value"));
		actionWidget = w;
		registerWidget(context, w, new ErrorWidget(), "action");
	}
	
	protected void configureCareSettingWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("careSetting", new HashMap<>());
		List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
		MetadataDropdownWidget<CareSetting> w = new MetadataDropdownWidget<>(careSettings, "");
		w.setInitialValue(HtmlFormEntryUtil.getCareSetting(config.get("value")));
		careSettingWidget = w;
		registerWidget(context, w, new ErrorWidget(), "careSetting");
	}
	
	protected void configureDosingTypeWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("dosingType", new HashMap<>());
		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option("", "", true));
		Class[] arr = { SimpleDosingInstructions.class, FreeTextDosingInstructions.class };
		for (Class c : arr) {
			w.addOption(new Option(c.getSimpleName(), c.getName(), false));
		}
		w.setInitialValue(config.get("value"));
		dosingTypeWidget = w;
		registerWidget(context, w, new ErrorWidget(), "dosingType");
	}
	
	protected void configureOrderTypeWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("orderType", new HashMap<>());
		List<OrderType> orderTypes = HtmlFormEntryUtil.getDrugOrderTypes();
		MetadataDropdownWidget<OrderType> w = new MetadataDropdownWidget<>(orderTypes, null);
		String defaultVal = config.get("value");
		if (defaultVal != null) {
			w.setInitialMetadataValue(HtmlFormEntryUtil.getOrderType(defaultVal));
		} else {
			w.setInitialMetadataValue(HtmlFormEntryUtil.getDrugOrderType());
		}
		orderTypeWidget = w;
		registerWidget(context, w, new ErrorWidget(), "orderType");
	}
	
	protected void configureDosingInstructionsWidget(FormEntryContext context) {
		dosingInstructionsWidget = configureTextWidget(context, "dosingInstructions");
	}
	
	protected void configureDoseWidget(FormEntryContext context) {
		doseWidget = configureNumericWidget(context, "dose", true);
	}
	
	protected void configureDoseUnitsWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("doseUnits", new HashMap<>());
		List<Concept> concepts = Context.getOrderService().getDrugDosingUnits();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		doseUnitsWidget = w;
		registerWidget(context, w, new ErrorWidget(), "doseUnits");
	}
	
	protected void configureRouteWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("route", new HashMap<>());
		List<Concept> concepts = Context.getOrderService().getDrugRoutes();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		routeWidget = w;
		registerWidget(context, w, new ErrorWidget(), "route");
	}
	
	protected void configureFrequencyWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("frequency", new HashMap<>());
		List<OrderFrequency> frequencies = Context.getOrderService().getOrderFrequencies(false);
		MetadataDropdownWidget<OrderFrequency> w = new MetadataDropdownWidget<>(frequencies, "");
		w.setInitialMetadataValue(HtmlFormEntryUtil.getOrderFrequency(config.get("value")));
		frequencyWidget = w;
		registerWidget(context, w, new ErrorWidget(), "frequency");
	}
	
	protected void configureAsNeededWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("asNeeded", new HashMap<>());
		String checkboxValue = config.getOrDefault("value", "true");
		CheckboxWidget w = new CheckboxWidget(config.get("label"), checkboxValue);
		asNeededWidget = w;
		registerWidget(context, w, new ErrorWidget(), "asNeeded");
	}
	
	protected void configureInstructionsWidget(FormEntryContext context) {
		instructionsWidget = configureTextWidget(context, "instructions");
	}
	
	protected void configureUrgencyWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("urgency", new HashMap<>());
		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option("", "", true));
		for (Order.Urgency u : Order.Urgency.values()) {
			w.addOption(new Option(u.name(), u.name(), false));
		}
		w.setInitialValue(config.get("value"));
		urgencyWidget = w;
		registerWidget(context, w, new ErrorWidget(), "urgency");
	}
	
	protected void configureScheduledDateWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("scheduledDate", new HashMap<>());
		DateWidget w = new DateWidget();
		scheduledDateWidget = w;
		registerWidget(context, w, new ErrorWidget(), "scheduledDate");
	}
	
	protected void configureDurationWidget(FormEntryContext context) {
		durationWidget = configureNumericWidget(context, "duration", false);
	}
	
	protected void configureDurationUnitsWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("durationUnits", new HashMap<>());
		List<Concept> concepts = Context.getOrderService().getDurationUnits();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		durationUnitsWidget = w;
		registerWidget(context, w, new ErrorWidget(), "durationUnits");
	}
	
	protected void configureQuantityWidget(FormEntryContext context) {
		quantityWidget = configureNumericWidget(context, "quantity", true);
	}
	
	protected void configureQuantityUnitsWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getOrDefault("quantityUnits", new HashMap<>());
		List<Concept> concepts = Context.getOrderService().getDrugDispensingUnits();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		quantityUnitsWidget = w;
		registerWidget(context, w, new ErrorWidget(), "quantityUnits");
	}
	
	protected void configureNumRefillsWidget(FormEntryContext context) {
		String p = "numRefills";
		numRefillsWidget = configureNumericWidget(context, p, false);
	}
	
	protected String registerWidget(FormEntryContext context, Widget widget, ErrorWidget errorWidget, String propertyName) {
		String widgetId = context.registerWidget(widget);
		context.registerErrorWidget(widget, errorWidget);
		widgetReplacements.put(propertyName, widget);
		return widgetId;
	}
	
	protected Widget configureTextWidget(FormEntryContext context, String property) {
		Map<String, String> config = widgetConfig.getOrDefault(property, new HashMap<>());
		TextFieldWidget w = new TextFieldWidget();
		w.setInitialValue(config.get("value"));
		if (config.get("textArea") != null) {
			w.setTextArea(Boolean.parseBoolean(config.get("textArea")));
		}
		if (config.get("textAreaRows") != null) {
			w.setTextAreaRows(Integer.parseInt(config.get("textAreaRows")));
		}
		if (config.get("textAreaColumns") != null) {
			w.setTextAreaColumns(Integer.parseInt(config.get("textAreaColumns")));
		}
		if (config.get("textFieldSize") != null) {
			w.setTextFieldSize(Integer.parseInt(config.get("textFieldSize")));
		}
		if (config.get("textFieldMaxLength") != null) {
			w.setTextFieldSize(Integer.parseInt(config.get("textFieldMaxLength")));
		}
		if (config.get("placeholder") != null) {
			w.setPlaceholder(config.get("placeholder"));
		}
		registerWidget(context, w, new ErrorWidget(), "dosingInstructions");
		return w;
	}
	
	protected Widget configureNumericWidget(FormEntryContext context, String property, boolean allowDecimal) {
		Map<String, String> config = widgetConfig.getOrDefault(property, new HashMap<>());
		NumberFieldWidget w = new NumberFieldWidget(0d, null, allowDecimal);
		String defaultVal = config.get("value");
		if (defaultVal != null) {
			if (allowDecimal) {
				w.setInitialValue(Double.parseDouble(defaultVal));
			} else {
				w.setInitialValue(Integer.parseInt(defaultVal));
			}
		}
		registerWidget(context, w, new ErrorWidget(), property);
		return w;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		String ret = template;
		for (String property : widgetReplacements.keySet()) {
			Widget w = widgetReplacements.get(property);
			Map<String, String> c = widgetConfig.get(property);
			if (c != null) {
				String key = c.toString();
				ret = ret.replace(key, w.generateHtml(context));
			}
		}
		return ret;
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (DrugOrder) initialValue;
		// TODO: Populate initial values for various widgets using properties of initialValue if non-null
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		// TODO: Implement this to return an appropriate value using the configured widgets
		return null;
	}
	
	public DrugOrderAnswer getDrugOrderAnswer() {
		return drugOrderAnswer;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public Map<String, Map<String, String>> getWidgetConfig() {
		return widgetConfig;
	}
	
	public Map<String, Widget> getWidgetReplacements() {
		return widgetReplacements;
	}
	
	public Widget getDrugWidget() {
		return drugWidget;
	}
	
	public Widget getActionWidget() {
		return actionWidget;
	}
	
	public Widget getCareSettingWidget() {
		return careSettingWidget;
	}
	
	public Widget getDosingTypeWidget() {
		return dosingTypeWidget;
	}
	
	public Widget getOrderTypeWidget() {
		return orderTypeWidget;
	}
	
	public Widget getDosingInstructionsWidget() {
		return dosingInstructionsWidget;
	}
	
	public Widget getDoseWidget() {
		return doseWidget;
	}
	
	public Widget getDoseUnitsWidget() {
		return doseUnitsWidget;
	}
	
	public Widget getRouteWidget() {
		return routeWidget;
	}
	
	public Widget getFrequencyWidget() {
		return frequencyWidget;
	}
	
	public Widget getAsNeededWidget() {
		return asNeededWidget;
	}
	
	public Widget getInstructionsWidget() {
		return instructionsWidget;
	}
	
	public Widget getUrgencyWidget() {
		return urgencyWidget;
	}
	
	public Widget getScheduledDateWidget() {
		return scheduledDateWidget;
	}
	
	public Widget getDurationWidget() {
		return durationWidget;
	}
	
	public Widget getDurationUnitsWidget() {
		return durationUnitsWidget;
	}
	
	public Widget getQuantityWidget() {
		return quantityWidget;
	}
	
	public Widget getQuantityUnitsWidget() {
		return quantityUnitsWidget;
	}
	
	public Widget getNumRefillsWidget() {
		return numRefillsWidget;
	}
}
