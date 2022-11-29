package TicketService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

public class TicketServiceImplTest {

    // Importing the Class being tested
    private TicketServiceImpl underTest;

    @Rule
    public ExpectedException ticketRequestExceptions = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        // Ticket Payment Service
        TicketPaymentServiceImpl ticketPay = mock(TicketPaymentServiceImpl.class);

        // Seat Reservation Service
        SeatReservationService seatReserver = mock(SeatReservationService.class);

        // Now Initializing the Ticket Service being tested
        underTest = new TicketServiceImpl(ticketPay, seatReserver);

    }

    // can validate the ticket price by multiplying the
    // number of people that are in attendance (adult+child)
    // and multiplying them by the adult ticket price
    // then if the ticket price is > 0 and <= max attendance price
    // the transaction is valid


    @Test
    public void nullTicketOrder_InvalidPurchaseThrown() {
        // A valid Account Number
        Long myAccount = 4500821L;

        // This is my way of rejecting and invalid ticket purchase requests
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("No Valid Ticket Type Request was provided!");

        // Sending a null object in place of TicketTypeRequest
        // For purchasing
        underTest.purchaseTickets(myAccount, null);
    }


    @Test
    public void emptyTicketRequest_TicketServiceRejects() {
        // Given
        // An empty 'TicketTypeRequest'
        TicketTypeRequest newTicket = null;

        // Arrange
        // A valid Account Number
        Long myAccount = 4500821L;

        // When
        // The Empty Ticket Request and Valid Bank Account
        // is passed to the 'purchaseTicket' method
        // Needs to throw an Exception
        // This is my way of rejecting and invalid ticket purchase requests
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("No Valid Ticket Type Request was provided!");

        // Act
        // Executing the method that will cause the Exception
        // Also Asserting that this result was expected
        underTest.purchaseTickets(myAccount, newTicket);
    }


    @Test
    // This test was to ensure that
    // a valid account ID number is presented
    public void invalidAccountNumber_PurchaseRejected() {
        // Invalid Account number
        Long accountNum = -150L;

        // Instantiating the Ticket Request(s)
        TicketTypeRequest validRequest = new TicketTypeRequest(ADULT, 1);

        // Setting up the Exception Test
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("The account provided is not authentic, please enter a real account number.");

        // Sending the invalid account & valid ticketRequest
        underTest.purchaseTickets(accountNum,validRequest);
    }


    @Test
    public void validRequests_TicketCostCalculation() {
        // Given
        // Valid TicketTypeRequest
        int ticketAmount = 1;

        // Instantiating the Ticket Request
        TicketTypeRequest validRequest = new TicketTypeRequest(ADULT, ticketAmount);

        // A valid Account Number
        Long myAccount = 4500821L;

        // Arrange
        // When
        try {
            // Ticket is purchased
            underTest.purchaseTickets(myAccount, validRequest);

            // Then ticketOvercharge Check will be performed on calculated total
        } catch (InvalidPurchaseException badTotal){
            // Fail the test if the ticketOvercharge method causes an InvalidPurchaseException
            fail("The calculated Total may be overcharged!");
        }

    }


    @Test
    public void validRequests_SingleChildTicketRejected() {
        // Given
        // Valid TicketTypeRequest
        int ticketAmount = 1;

        // Instantiating the Ticket Request
        TicketTypeRequest invalidRequest = new TicketTypeRequest(CHILD, ticketAmount);

        // A valid Account Number
        Long myAccount = 4500821L;

        // Arrange
        // Expected Exception
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("Infant and child tickets can't be purchased without an adult ticket being purchased!");

        // When
        // The Request is made for Payment
        underTest.purchaseTickets(myAccount,invalidRequest);
    }

    @Test
    public void validRequests_SingleInfantTicketRejected() {
        // Instantiating the Ticket Request
        TicketTypeRequest invalidRequest = new TicketTypeRequest(INFANT, 1);

        // A valid Account Number
        Long myAccount = 4500821L;

        // Arrange
        // Expected Exception
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("Infant and child tickets can't be purchased without an adult ticket being purchased!");

        // When
        // The Request is made for Payment
        underTest.purchaseTickets(myAccount,invalidRequest);
    }


    @Test
    public void TicketPurchaseWithoutAdult_InvalidPurchaseException() {
        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, 1);
        TicketTypeRequest validCRequest = new TicketTypeRequest(CHILD, 1);

        // A valid Account Number
        Long myAccount = 2397844L;

        // Act
        // Expected Exception
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("Infant and child tickets can't be purchased without an adult ticket being purchased!");

        // When
        // The Request is made for Payment
        underTest.purchaseTickets(myAccount,validIRequest,validCRequest);
    }


    @Test
    public void validTicketRequests_FamilyOfThreeAccepted() {
        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, 1);
        TicketTypeRequest validARequest = new TicketTypeRequest(ADULT, 1);
        TicketTypeRequest validCRequest = new TicketTypeRequest(CHILD, 1);

        // A valid Account Number
        Long myAccount = 2397844L;


        // When
        // Family Ticket is purchased
        try {
            // Ticket is purchased
            underTest.purchaseTickets(myAccount, validIRequest, validARequest, validCRequest);

            // Then ticketOvercharge Check will be performed on calculated total
        } catch (InvalidPurchaseException badTotal){

            // Fail the test if the ticketOvercharge method causes an InvalidPurchaseException
            fail("The calculated Total may be overcharged!");
        }
    }


    @Test
    public void largeFamily_MoreInfantsThanAdults_PurchaseRejected() {
        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, 2);
        TicketTypeRequest validARequest = new TicketTypeRequest(ADULT, 1);

        // A valid Account Number
        Long myAccount = 2397844L;

        // Expected Exception
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("There can't be more Infants than Adults in the cinema theatre!");

        // The Request is made for Payment
        underTest.purchaseTickets(myAccount,validIRequest,validARequest);
    }


    // This will be no ration limit. (for every 1 adult, there can be up to 1 infant & any number of child tickets.)
    @Test
    public void largeFamily_LogicValid_PurchaseAccepted() {
        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, 1);
        TicketTypeRequest validARequest = new TicketTypeRequest(ADULT, 2);
        TicketTypeRequest validCRequest = new TicketTypeRequest(CHILD, 3);

        // A valid Account Number
        Long myAccount = 2397844L;

        // the total cost of the ticket request
        try {
            // Ticket is purchased
            underTest.purchaseTickets(myAccount, validIRequest, validARequest, validCRequest);

            // Then ticketOvercharge Check will be performed on calculated total
        } catch (InvalidPurchaseException badTotal){

            // Fail the test if the ticketOvercharge method causes an InvalidPurchaseException
            fail("The calculated Total may be overcharged!");
        }
    }


    @Test
    public void largeFamily_TicketOrderAccepted_PurchaseMade() {
        // Given
        // Valid Family of Three TicketTypeRequest
        int childAmount = 4;
        int infantAmount = 3;
        int ticketAmount = 3;

        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, infantAmount);
        TicketTypeRequest validARequest = new TicketTypeRequest(ADULT, ticketAmount);
        TicketTypeRequest validCRequest = new TicketTypeRequest(CHILD, childAmount);

        // Arrange
        // A valid Account Number
        Long myAccount = 2397844L;


        // When
        // Ticket is purchased
        try {
            // Ticket is purchased
            underTest.purchaseTickets(myAccount, validIRequest, validARequest, validCRequest);

            // Then ticketOvercharge Check will be performed on calculated total
        } catch (InvalidPurchaseException badTotal){

            // Fail the test if the ticketOvercharge method causes an InvalidPurchaseException
            fail("The calculated Total may be overcharged!");
        }

    }

    
    @Test
    public void largeParty_TicketLimitExceeded_PurchaseRejected() {
        // Ticket group larger than 20 tickets
        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, 4);
        TicketTypeRequest validARequest = new TicketTypeRequest(ADULT, 12);
        TicketTypeRequest validCRequest = new TicketTypeRequest(CHILD, 8);

        // A valid Account Number
        Long myAccount = 2397844L;

        // Expected Exception
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("A Maximum of 20 tickets can be purchased per group!");

        // The Request is made for Payment
        underTest.purchaseTickets(myAccount, validARequest, validIRequest, validCRequest);
    }


    @Test
    public void largeFamily_AllTicketsNull_PurchaseRejected() {
        // Null Ticket Request
        TicketTypeRequest invalidTicket = null;

        // A valid Account Number
        Long myAccount = 2397844L;

        // Expected Exception
        ticketRequestExceptions.expect(InvalidPurchaseException.class);
        ticketRequestExceptions.expectMessage("Infant and child tickets can't be purchased without an adult ticket being purchased!");

        // The Request is made for Payment
        underTest.purchaseTickets(myAccount, invalidTicket, invalidTicket, invalidTicket);
    }


    @Test
    public void largeFamily_NullTicketsIncluded_PurchaseMade() {
        // Null Ticket Request
        TicketTypeRequest invalidTicket = null;

        // Instantiating the Ticket Request(s)
        TicketTypeRequest validIRequest = new TicketTypeRequest(INFANT, 3);
        TicketTypeRequest validARequest = new TicketTypeRequest(ADULT, 5);
        TicketTypeRequest validCRequest = new TicketTypeRequest(CHILD, 6);

        // A valid Account Number
        Long myAccount = 2397844L;

        // Failing happens if an exception is thrown
        // the total cost of the ticket request
        try{
            // When
            // Ticket is purchased
            underTest.purchaseTickets(myAccount, validIRequest, invalidTicket, validARequest, invalidTicket, validCRequest);
        }
        catch(InvalidPurchaseException badCalculation){
            // Displaying the Error
            System.out.println(badCalculation.toString());
            System.out.println();

            // Cause the Failure
            fail("This method should not have thrown an Invalid Purchase Exception!");
        }
    }

}
