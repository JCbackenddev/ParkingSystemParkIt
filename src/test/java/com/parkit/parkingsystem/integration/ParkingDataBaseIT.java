package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;


@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    
    @Mock
    private static Ticket ticket;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
    	dataBasePrepareService.clearDataBaseEntries();
    }
    
    @Test
    @DisplayName("Tests if the car can be found in the system")
    public void parkingACarTest() throws Exception{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        String testRegistrationNumber = "ABCDEF";
        
        // ACT
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        
        // ASSERT
        assertEquals(ticket.getVehicleRegNumber(), testRegistrationNumber);
        
    }
    
    @Test
    @DisplayName("Tests if the parking spot availability is set to false")
    public void parkingTableAvailabilityTest() throws Exception{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
        // ACT
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        // ASSERT
        assertFalse(ticket.getParkingSpot().isAvailable());
    }
    
    @Test
    @DisplayName("Tests if a price is set in the DB")
    public void parkingLotExitPriceTest() throws ClassNotFoundException{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
        //ACT 
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        // ASSERT
        
        assertNotNull(ticket.getPrice());
    }
    
    @Test
    @DisplayName("Tests if an OutTime is set in the DB")
    public void parkingLotExitOutTimeTest() throws ClassNotFoundException{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
        //ACT 
        parkingService.processIncomingVehicle();
        
        try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        
        // ASSERT
        assertNotNull(ticket.getOutTime());
    }
    
    @Test
    @DisplayName("Tests if the system recognizes a returning user")
    public void inputReaderUtilReturningUserTest() throws ClassNotFoundException{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
        //ACT 
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        
        // ASSERT
        assertTrue(parkingService.getIfReturningUser("ABCDEF"));
    }
    
    @Test
    @DisplayName("Tests if the returning users are greeted properly")
    public void inputReaderUtilReturningUserWelcomeMessageTest() throws ClassNotFoundException{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        
        //ACT 
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        
        parkingService.getIfReturningUser("ABCDEF");
        
        String out = null;
		try {
			out = outContent.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
        // ASSERT
        Assert.assertThat(out, containsString("Welcome back! As a recurring user of our parking lot, you'll benefit from a 5% discount."));
    }
    
    @Test
    @DisplayName("Tests if the returning users are greeted properly")
    public void processExitingVehicleMessageTest() throws Exception{
    	//ARRANGE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        
        //ACT 
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        
        String out = null;
		try {
			out = outContent.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
        // ASSERT
        Assert.assertThat(out, containsString("Please pay the parking fare:"));
        Assert.assertThat(out, containsString("Recorded out-time for vehicle number:"));
    }

}
