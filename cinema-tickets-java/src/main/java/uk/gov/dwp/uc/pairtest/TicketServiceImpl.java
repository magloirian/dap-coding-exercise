package uk.gov.dwp.uc.pairtest;


import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

import java.util.ArrayList;
import java.util.List;


public class TicketServiceImpl implements TicketService {

    private final SeatReservationService seatingPlatform;
    private final TicketPaymentServiceImpl payPlatform;

    /**
     * Should only have private methods other than the one below.
     */

    // Public Constructor (Should not could under the methods constraint given the class is public)
    // And the only way to test the behaviour closely will be to have a public constructor
    public TicketServiceImpl(TicketPaymentServiceImpl paymentPlatform, SeatReservationService reserveSeat) {
        // Now this allows me to make payment requests
        this.payPlatform = paymentPlatform;

        // And Seat Reservation Requests
        this.seatingPlatform = reserveSeat;
    }


    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        // Important variables for the validation
        // of the Ticket Requests and Account Details
        List<Integer> ticketOrder;
        boolean accountisValid;

        // Important variables for Ticket Monitoring
        int totalTicketCost, totalSeatReservation;
        int aTicket = 0, cTicket = 0, iTicket = 0;

        // Analysing the single/set of ticketTypeRequests
        // Refactoring into a separate method
        // returns the no. of adult, child & infant
        ticketOrder = resolveTicketOrder(ticketTypeRequests);

        // Now setting the Ticket Composure
        aTicket = ticketOrder.get(0);
        cTicket = ticketOrder.get(1);
        iTicket = ticketOrder.get(2);


        // Now Checking the validity of the account
        // Refactoring into a separate method
        accountisValid = validateAccountDetails(accountId);

        // Only if the Account Details Validator
        // Returns 'false' should an error be thrown
        if(!accountisValid) {
            // Throw the Exception
            throw new InvalidPurchaseException("The account provided is not authentic, please enter a real account number.");
        }

        // Now calculating the cost of the Ticket
        totalTicketCost = calculateTicketCost(aTicket, cTicket);

        // Now calculating the number of seats to reserve
        totalSeatReservation = (aTicket + cTicket);

        // Last Important Steps
        // Now paying for the seats and reserving them
        payTicket(payPlatform, accountId, totalTicketCost);

        // Now reserving the seats
        reserveSeat(seatingPlatform, accountId, totalSeatReservation);

    }



    private int calculateTicketCost(int aTicket, int cTicket) {
        // Important calculation constants
        final int childTicket = 10, adultTicket = 20;

        // The variable that will hold the total cost
        int totalTicketCost;

        // Now to calculate the total Ticket Cost
        totalTicketCost = (adultTicket * aTicket) + (childTicket * cTicket);

        // Default Return value
        // Stating the function has not been implemented
        return totalTicketCost;
    }

    // This method will validate the Account Details
    // before the Tickets can be purchased
    private boolean validateAccountDetails(Long accountId) {

        // Incorrect / Inauthentic Account Details given
        if(accountId <= 0) {
            // return False
            return false;
        }

        // Meaning the account has passed
        // all authentication tests
        return true;
    }


    // This method will validate the Ticket Request
    // Simplify the conditionals
    // which includes removing 'null' ticket requests
    // And could also see whether the group of ticket request are feasible
    private List<Integer> resolveTicketOrder(TicketTypeRequest[] ticketTypeRequests) {

        // Monitoring of Ticket Request Group Size
        final int noTickets = 0, ticketLimit = 20;
        int numTickets = 0, totalTicketGroup = 0;

        // Important variables to keep track of the ticket composure
        int childTickets = 0, adultTickets = 0, infantTickets = 0;


        // Null Ticket Cases:
        if (ticketTypeRequests == null || ticketTypeRequests.length == 1 && ticketTypeRequests[noTickets] == null) {
            // then throw error message
            throw new InvalidPurchaseException("No Valid Ticket Type Request was provided!");
        }

        // Multiple Ticket Validation Cases:
        // Gathering Ticket Composure
        for(int i = 0; i < ticketTypeRequests.length; i++) {

            // Conditional to ignore the null Ticket Requests
            if (ticketTypeRequests[i] != null) {

                // Getting the Type of Tickets Requested
                TicketTypeRequest.Type currentTicket = ticketTypeRequests[i].getTicketType();

                // Need to check the No. of tickets requested
                numTickets = ticketTypeRequests[i].getNoOfTickets();

                // Conditionally Updating the Ticket Type Totals
                if (currentTicket == ADULT) {
                    // Increment 'adultTickets'
                    adultTickets += numTickets;
                }

                if (currentTicket == CHILD) {
                    // Increment 'childTickets'
                    childTickets += numTickets;
                }

                if (currentTicket == INFANT) {
                    // Increment 'infantTickets'
                    infantTickets += numTickets;
                }

                // Now calculating the totalTicketGroup
                totalTicketGroup += numTickets;
            }
        }


        // Conditional to prevent 'Child' and 'Infant' ticket purchases
        // without 'Adult' tickets being purchased in appropriate proportion
        if (adultTickets == noTickets) {

            // Throw the exception
            throw new InvalidPurchaseException("Infant and child tickets can't be purchased without an adult ticket being purchased!");
        }


        // cases where infant tickets
        // exceed adult tickets
        if (adultTickets < infantTickets) {

            // Throw the exception
            throw new InvalidPurchaseException("There can't be more Infants than Adults in the cinema theatre!");
        }


        // cases where the 20 tickets limit is exceeded
        if (totalTicketGroup > ticketLimit) {

            // Throw the exception
            throw new InvalidPurchaseException("A Maximum of 20 tickets can be purchased per group!");
        }


        // Now to pass the ticket composure back via list
        List<Integer> ticketComposure = new ArrayList<>();

        // Setting the ticket composure list
        ticketComposure.add(adultTickets);
        ticketComposure.add(childTickets);
        ticketComposure.add(infantTickets);

        // Means the Ticket Request is valid
        return ticketComposure;
    }

    // Ticket Payment Request
    private void payTicket(TicketPaymentServiceImpl paymentPlatform, long accountID, int ticketTotal) {
        // Paying
        paymentPlatform.makePayment(accountID, ticketTotal);
    }

    // Seat Reservation Request
    private void reserveSeat(SeatReservationService seatReserve, long accountID, int totalSeats) {
        // Reserving seats
        seatReserve.reserveSeat(accountID, totalSeats);
    }

}
