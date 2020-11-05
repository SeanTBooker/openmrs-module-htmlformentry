package org.openmrs.module.htmlformentry.element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.tester.DrugOrderFieldTester;
import org.openmrs.module.htmlformentry.tester.FormResultsTester;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class DrugOrderSubmissionElementTest extends BaseHtmlFormEntryTest {

	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}

	@Test
	public void shouldPopulateExistingOrders() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openExistingToView(3);
		DrugOrderSubmissionElement e = formSessionTester.getSubmissionAction(DrugOrderSubmissionElement.class).get(0);
		List<DrugOrder> existingOrders = e.getExistingOrders();
		assertThat(existingOrders.size(), is(2));
	}

	@Test
	public void shouldFailValidationIfMissingPreviousOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		DrugOrderFieldTester revisedTriomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		revisedTriomuneField.orderAction("REVISE");
		revisedTriomuneField.freeTextDosing("My instructions");
		FormResultsTester revisedResults = formSessionTester.submitForm();
		revisedResults.assertErrorMessage("htmlformentry.drugOrderError.previousOrderRequired");
	}

	@Test
	public void shouldFailValidationIfDrugChangedOnRevision() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openExistingToEdit(3);
		DrugOrderFieldTester revisedTriomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		revisedTriomuneField.orderAction("REVISE").previousOrder("111"); // This has drug 3, which is different
		FormResultsTester revisedResults = formSessionTester.submitForm();
		revisedResults.assertErrorMessage("htmlformentry.drugOrderError.drugChangedForRevision");
	}

	@Test
	public void shouldFailValidationIfDosingChangedOnRenew() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openExistingToEdit(3);
		DrugOrderFieldTester revisedTriomuneField = DrugOrderFieldTester.forDrug(3, formSessionTester);
		revisedTriomuneField.orderAction("RENEW").previousOrder("111");
		revisedTriomuneField.freeTextDosing("My instructions");
		FormResultsTester revisedResults = formSessionTester.submitForm();
		revisedResults.assertErrorMessage("htmlformentry.drugOrderError.dosingChangedForRenew");
	}
	
	@Test
	public void shouldCreateNewInpatientAndOutpatientOrders() {
		for (String careSettingId : new String[] { "1", "2" }) {
			FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting(careSettingId).urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			triomuneField.quantity("10").quantityUnits("51").numRefills("1");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
			DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
			assertThat(order.getCareSetting().getId().toString(), is(careSettingId));
		}
	}
	
	@Test
	public void shouldCreateNewFreeTextOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		results.assertFreeTextDosing(order, "Triomune instructions");
		results.assertSimpleDosingFieldsAreNull(order);
		results.assertDurationFieldsNull(order);
		results.assertDispensingFieldsNull(order);
	}
	
	@Test
	public void shouldCreateNewSimpleOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.simpleDosing("2", "51", "1", "22");
		triomuneField.asNeeded("true").instructions("TBD");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		results.assertSimpleDosing(order, 2.0, 51, 1, 22, true, "TBD");
		results.assertFreeTextDosingFieldsNull(order);
		results.assertDurationFieldsNull(order);
		results.assertDispensingFieldsNull(order);
	}
	
	@Test
	public void shouldCreateRoutineOrderWithDefaultDateActivatedAndOrderer() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
		assertThat(order.getOrderer().getPerson().getId(), is(502));
	}
	
	@Test
	public void shouldCreateRoutineOrderWithDateActivatedAsEntryDate() {
		FormTester formTester = FormTester.buildForm("drugOrderTestFormDateActivatedEntryDate.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), TestUtil.formatYmd(order.getDateCreated()));
	}
	
	@Test
	public void shouldCreateRoutineOrderWithSpecificDateActivated() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.dateActivated("2020-04-15");
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-04-15 00:00:00");
	}
	
	@Test
	public void shouldCreateScheduledOrder() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2");
		triomuneField.urgency(Order.Urgency.ON_SCHEDULED_DATE.name()).scheduledDate("2020-05-02");
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1);
		DrugOrder order = results.assertDrugOrder(Order.Action.NEW, 2);
		TestUtil.assertDate(order.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
		TestUtil.assertDate(order.getScheduledDate(), "yyyy-MM-dd HH:mm:ss", "2020-05-02 00:00:00");
		assertThat(order.getUrgency(), is(Order.Urgency.ON_SCHEDULED_DATE));
	}
	
	@Test
	public void reviseShouldCloseAndLinkPrevious() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		
		Integer initialOrderId;
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o1 = results.assertDrugOrder(Order.Action.NEW, 2);
			TestUtil.assertDate(o1.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
			assertThat(o1.getDosingInstructions(), is("Triomune instructions"));
			assertThat(o1.getDateStopped(), nullValue());
			initialOrderId = o1.getOrderId();
		}
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-05-15", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.orderAction("REVISE").previousOrder(initialOrderId.toString());
			triomuneField.freeTextDosing("Revised Triomune instructions");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o2 = results.assertDrugOrder(Order.Action.REVISE, 2);
			TestUtil.assertDate(o2.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-05-15 00:00:00");
			assertThat(o2.getDateStopped(), nullValue());
			
			DrugOrder o1 = (DrugOrder) Context.getOrderService().getOrder(initialOrderId);
			assertThat(o2.getPreviousOrder(), is(o1));
			TestUtil.assertDate(o1.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-05-14 23:59:59");
			assertThat(o1.getDosingInstructions(), is("Triomune instructions"));
			assertThat(o2.getDosingInstructions(), is("Revised Triomune instructions"));
		}
	}
	
	@Test
	public void renewShouldCloseAndLinkPrevious() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		
		Integer initialOrderId;
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting("1").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			triomuneField.quantity("30").quantityUnits("51").numRefills("2");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o1 = results.assertDrugOrder(Order.Action.NEW, 2);
			TestUtil.assertDate(o1.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
			results.assertDispensing(o1, 30.0, 51, 2);
			assertThat(o1.getDateStopped(), nullValue());
			initialOrderId = o1.getOrderId();
		}
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-05-15", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.careSetting("1").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.orderAction("RENEW").previousOrder(initialOrderId.toString());
			triomuneField.freeTextDosing("Triomune instructions");
			triomuneField.quantity("50").quantityUnits("51").numRefills("3");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o2 = results.assertDrugOrder(Order.Action.RENEW, 2);
			TestUtil.assertDate(o2.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-05-15 00:00:00");
			assertThat(o2.getDateStopped(), nullValue());
			
			DrugOrder o1 = (DrugOrder) Context.getOrderService().getOrder(initialOrderId);
			assertThat(o2.getPreviousOrder(), is(o1));
			TestUtil.assertDate(o1.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-05-14 23:59:59");
			assertThat(o2.getDosingInstructions(), is(o1.getDosingInstructions()));
			results.assertDispensing(o2, 50.0, 51, 3);
		}
	}
	
	@Test
	public void discontinueShouldCloseAndLinkPrevious() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		
		Integer initialOrderId;
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-03-30", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.freeTextDosing("Triomune instructions");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o1 = results.assertDrugOrder(Order.Action.NEW, 2);
			TestUtil.assertDate(o1.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-03-30 00:00:00");
			assertThat(o1.getDateStopped(), nullValue());
			initialOrderId = o1.getOrderId();
		}
		{
			FormSessionTester formSessionTester = formTester.openNewForm(6);
			formSessionTester.setEncounterFields("2020-05-15", "2", "502");
			DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
			triomuneField.careSetting("2").urgency(Order.Urgency.ROUTINE.name());
			triomuneField.orderAction("DISCONTINUE").previousOrder(initialOrderId.toString());
			triomuneField.discontinueReason("556");
			FormResultsTester results = formSessionTester.submitForm();
			results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
			DrugOrder o2 = results.assertDrugOrder(Order.Action.DISCONTINUE, 2);
			TestUtil.assertDate(o2.getDateActivated(), "yyyy-MM-dd HH:mm:ss", "2020-05-15 00:00:00");
			assertThat(o2.getDateStopped(), nullValue());
			
			DrugOrder o1 = (DrugOrder) Context.getOrderService().getOrder(initialOrderId);
			assertThat(o2.getPreviousOrder(), is(o1));
			TestUtil.assertDate(o1.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-05-14 23:59:59");
		}
	}

	@Test
	public void reviseShouldVoidIfWithinSameEncounterForSameDate() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester triomuneField = DrugOrderFieldTester.forDrug(2, formSessionTester);
		triomuneField.orderAction("NEW").careSetting("2").urgency(Order.Urgency.ROUTINE.name());
		triomuneField.freeTextDosing("Triomune instructions");
		FormResultsTester results = formSessionTester.submitForm();
		results.assertNoErrors().assertEncounterCreated().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
		DrugOrder originalOrder = results.assertDrugOrder(Order.Action.NEW, 2);

		FormSessionTester reviseTester = formSessionTester.reopenForEditing(results);
		DrugOrderFieldTester revisedTriomuneField = DrugOrderFieldTester.forDrug(2, reviseTester);
		revisedTriomuneField.orderAction("REVISE").previousOrder(originalOrder.getId().toString());
		revisedTriomuneField.freeTextDosing("Revised Triomune instructions");
		FormResultsTester revisedResults = reviseTester.submitForm();

		// Since it was voided, then this should not be a revise with previous order, but void with new order
		revisedResults.assertNoErrors().assertOrderCreatedCount(2).assertNonVoidedOrderCount(1).assertVoidedOrderCount(1);
		DrugOrder revisedOrder = results.assertDrugOrder(Order.Action.NEW, 2);
		assertThat(revisedOrder.getPreviousOrder(), nullValue());
		assertThat(revisedOrder.getDosingInstructions(), is("Revised Triomune instructions"));

		Order originalOrderRetrieved = Context.getOrderService().getOrder(originalOrder.getId());
		assertThat(originalOrderRetrieved.getVoided(), is(true));
		assertThat(originalOrderRetrieved.getVoidReason(), is("Voided by htmlformentry"));
	}

	@Test
	public void reviseShouldVoidAndAssumePreviousOrderIfApplicable() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");

		// Here, we revise on order in an earlier encounter

		FormSessionTester editSession1 = formTester.openNewForm(2); // Patient has an open order for drug 2
		editSession1.setEncounterFields("2020-03-30", "2", "502");
		DrugOrderFieldTester revision1 = DrugOrderFieldTester.forDrug(2, editSession1);
		revision1.urgency("ROUTINE").careSetting("1").orderAction("REVISE").previousOrder("3");
		revision1.freeTextDosing("My revision").quantity("1").quantityUnits("51").numRefills("0");
		FormResultsTester results1 = editSession1.submitForm();
		results1.assertNoErrors().assertOrderCreatedCount(1).assertNonVoidedOrderCount(1);
		DrugOrder order1 = results1.assertDrugOrder(Order.Action.REVISE, 2);
		assertThat(order1.getPreviousOrder().getOrderId(), is(3));

		// Then, we revise this above revision

		FormSessionTester editSession2 = editSession1.reopenForEditing(results1);
		DrugOrderFieldTester revision2 = DrugOrderFieldTester.forDrug(2, editSession2);
		revision2.orderAction("REVISE").previousOrder(order1.getId().toString());
		revision2.freeTextDosing("My revision revision");
		FormResultsTester results2 = editSession2.submitForm();

		// Since we are revising a matching order (encounter, drug, dateActivated match), this should void previous
		// But since the one we are voiding was itself a revision, this should be a REVISION of that order's previous
		Order voidedOrder = Context.getOrderService().getOrder(order1.getId());
		assertThat(voidedOrder.getVoided(), is(true));
		assertThat(voidedOrder.getVoidReason(), is("Voided by htmlformentry"));

		results2.assertNoErrors().assertOrderCreatedCount(2).assertNonVoidedOrderCount(1).assertVoidedOrderCount(1);
		DrugOrder order2 = results2.assertDrugOrder(Order.Action.REVISE, 2);
		assertThat(order2.getPreviousOrder().getOrderId(), is(3));
		assertThat(order2.getDosingInstructions(), is("My revision revision"));

		DrugOrder originalOrder = (DrugOrder)Context.getOrderService().getOrder(3);
		TestUtil.assertDate(originalOrder.getDateStopped(), "yyyy-MM-dd HH:mm:ss", "2020-03-29 23:59:59");
	}
}
