package com.endicott.edu.simulators;

import com.endicott.edu.datalayer.CollegeDao;
import com.endicott.edu.datalayer.NewsFeedDao;
import com.endicott.edu.datalayer.SportsDao;
import com.endicott.edu.models.*;
import com.endicott.edu.datalayer.StudentDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Responsible for simulating everything sports related.
 */
public class SportManager {
    SportsDao dao = new SportsDao();
    static private Logger logger = Logger.getLogger("SportManager");

    /**
     * Simulate the elaspe of time at the college.
     *
     * @param runId
     * @param hoursAlive number of hours since the college started.
     */
    public void handleTimeChange(String runId, int hoursAlive) {
        List<SportModel> sports = dao.getSports(runId);

        for (SportModel sport : sports) {
            calculateNumberOfPlayersOnTeam(runId, sport);
            fillUpTeamAndSetActiveStatus(runId, sport);
            billRunningCostofSport(runId, hoursAlive, sport);
            sport.setHourLastUpdated(hoursAlive);
            playGame(sport, hoursAlive, runId);
        }

        dao.saveAllSports(runId, sports);
    }

    /**
     * Charge the cost of running the sports since the last time
     * the bill was paid (the sport was updated).
     *
     * @param runId
     * @param hoursAlive
     * @param sport
     */
    private void billRunningCostofSport(String runId, int hoursAlive, SportModel sport) {
        int hoursSinceBillPaid = hoursAlive - sport.getHourLastUpdated();
        int newCharge = (int) (hoursSinceBillPaid * ((float) sport.getCostPerDay() / 24f));
        if(newCharge > 0)
        {
            Accountant.payBill(runId,"Charge for " + sport.getName(), newCharge);
        }
    }

    /**
     * Add a new sports team to the college.
     *
     * @param sportName
     * @param runId
     * @return
     */
    public static SportModel addNewTeam(String sportName, String runId){
        SportsDao newSportDao = new SportsDao();
        logger.info("Attempt to add sport: '" + sportName + "' to '" + runId + "'");
        SportModel sport = new SportModel();
        SportModel result = null;

        if (sportName.equals("Men's Basketball")){
            result = new SportModel(12, 0, 15, 100, 0, 0, 0, 20, 50000, 50, 0, "Men's Basketball", runId, 0, 48, "Male");
            Accountant.payBill(runId, "Men's Basketball start up fee", result.getStartupCost());
        }
        else if(sportName.equals("Women's Basketball")){
            result  = new SportModel(12, 0, 15, 100, 0,0,0,20,50000,50,0,"Women's Basketball", runId, 0,48, "Female");
            Accountant.payBill(runId, "Women's Basketball start up fee", result.getStartupCost());
        }
        else if(sportName.equals("Baseball")){
            result  = new SportModel(16, 0, 20, 100, 0,0,0,20,75000,50,0,"Baseball", runId, 0,48, "Male");
            Accountant.payBill(runId, "Baseball start up fee", result.getStartupCost());
        }
        else if(sportName.equals("Softball")){
            result  = new SportModel(16, 0, 20, 100, 0,0,0,20,75000,50,0,"Softball", runId, 0, 48,"Female");
            Accountant.payBill(runId, "Softball start up fee", result.getStartupCost());
        }
        else if(sportName.equals("Women's Soccer")){
            result  = new SportModel(15,0, 20, 100, 0, 0, 0 , 20 , 50000, 50, 0, "Women's Soccer", runId, 0,48, "Female" );
            Accountant.payBill(runId, "Women's Soccer start up fee", result.getStartupCost());
        }
        else if(sportName.equals("Men's Soccer")){
            result  = new SportModel(15,0, 20, 100, 0, 0, 0 , 20 , 50000, 50, 0, "Men's Soccer", runId, 0, 48,"Male" );
            Accountant.payBill(runId, "Men's Soccer start up fee", result.getStartupCost());
        } else {
            logger.severe("Could not add sport: '" + sportName + "'");
        }

        addPlayers(runId, result);
        calculateNumberOfPlayersOnTeam(runId, result);
        fillUpTeamAndSetActiveStatus(runId, result);
        newSportDao.saveNewSport(runId, result);

        return result;
    }

    /**
     * Set the number of players that are on the sport by looking through
     * all students to see who's on the team.
     *
     * @param runId
     * @param sport
     */
    public static void calculateNumberOfPlayersOnTeam(String runId, SportModel sport){
        StudentDao dao = new StudentDao();
        List<StudentModel> students = dao.getStudents(runId);
        for(int i = 0; i < students.size(); i++) {
            if(students.get(i).getTeam().equals(sport)){
                sport.setCurrentPlayers(sport.getCurrentPlayers() + 1);
            }
        }
    }

    /**
     * Fill the roster for the sport and mark the sport as active
     * if you have enough players.
     *
     * @param runId
     * @param sport
     */
    public static void fillUpTeamAndSetActiveStatus(String runId, SportModel sport){
        if (sport.getCurrentPlayers() < sport.getMinPlayers()){
            addPlayers(runId, sport);
            if(sport.getCurrentPlayers() < sport.getMinPlayers()){
                sport.setActive(0);
            }
            else{
                sport.setActive(1);
            }
        }
        else {
            sport.setActive(1);
        }
    }

    /**
     * Add players to a team until reaching the maximum number of
     * players allowed on the team.
     *
     * @param runId
     * @param sport
     * @return
     */
    public static SportModel addPlayers(String runId, SportModel sport){
        StudentDao dao = new StudentDao();
        List<StudentModel> students = dao.getStudents(runId);
        for(int i = 0; i < students.size(); i++) {
            if (students.get(i).isAthlete() && ((students.get(i).getTeam().equals("")) || students.get(i).getTeam().equals("unknown"))) {
                if (students.get(i).getGender().equals(sport.getGender())) {
                    if (sport.getCurrentPlayers() < sport.getMaxPlayers()) {
                        students.get(i).setTeam(sport.getSportName());
                        sport.setCurrentPlayers((sport.getCurrentPlayers() + 1));
                    }
                }
            }
        }

        dao.saveAllStudents(runId, students);
        return sport;

    }

    /**
     * Sell the sports team.
     *
     * @param runId
     */
    static public void sellSport(String runId) {
        SportsDao sportsDao = new SportsDao();
        NewsFeedDao noteDao = new NewsFeedDao();

        sportsDao.deleteSports(runId);
        noteDao.deleteNotes(runId);
    }

    /**
     * Return a list of sports representing the sports that can
     * be added to the college.  You can't add a sport that already
     * exists.
     *
     * @param runId
     * @return
     */
    public static ArrayList<SportModel> checkAvailableSports(String runId) {
        SportsDao dao = new SportsDao();
        CollegeDao cao = new CollegeDao();
        CollegeModel college = cao.getCollege(runId);
        int collegeFunds = college.getAvailableCash();

        // Creates a list called availbleSportNames of all sports names a college can make
        ArrayList<String> availableSportsNames = new ArrayList<>();
        for (int i = 0; i < dao.seeAllSportNames().size(); i++ ){
            availableSportsNames.add(dao.seeAllSportNames().get(i));
        }

        // Compares the currents sports names w the names in the availbleSportsNames array and takes out any sports that are already created
        for(int x = 0; x < dao.getSports(runId).size(); x++){
            for(int y = 0; y < availableSportsNames.size(); y++){
                if( availableSportsNames.get(y).equals(dao.getSports(runId).get(x).getName())){
                    availableSportsNames.remove(y);
                }
            }
        }

        // Takes the modified availbleSportsNames array and converts/creates objects of sport model with the left...
        // over names in availblesportsnames and stores them in abvaibleSports
        ArrayList<SportModel> availableSports = new ArrayList<>();
        for(int yz = 0; yz < availableSportsNames.size(); yz++){

            // TODO: we should check if the college has enough money to startup the sport.

            System.out.println(availableSportsNames.get(yz) + "This is a check");
            logger.info("list of the names after the check " + availableSportsNames.get(yz));
            SportModel tempSport = new SportModel();
            tempSport.setName(availableSportsNames.get(yz));
            availableSports.add(tempSport);
        }
        return availableSports;
    }

    /**
     * If enough time has elapsed since the last game, play a game.
     *
     * @param sport
     * @param hoursAlive
     * @param runId
     */
    public static void playGame(SportModel sport, int hoursAlive, String runId ){
        if (sport.getHoursUntilNextGame() <= 0) {
            simulateGame(sport, hoursAlive, runId);
            sport.setHoursUntilNextGame(48);
        } else {
            sport.setHoursUntilNextGame(Math.max(0, hoursAlive - sport.getHourLastUpdated()));
        }
    }

    /**
     * Delete the given sport.
     *
     * @param runId
     * @param sportName
     */
    public static void deleteSelectedSport(String runId, String sportName){
        SportsDao dao = new SportsDao();
        dao.deleteSelectedSport(runId, sportName);
    }

    /**
     * Simulate the playing of a game.
     *
     * @param sport
     * @param hoursAlive
     * @param runId
     */
    public static void simulateGame(SportModel sport, int hoursAlive,String runId){
        StudentDao stuDao = new StudentDao();
        List<StudentModel> students = stuDao.getStudentsOnSport(runId,sport.getName());
        int numOfPlayers = sport.getCurrentPlayers();
        int totalAthleticAbility = 0;

        for (StudentModel student : students) {
            totalAthleticAbility = totalAthleticAbility + student.getAthleticAbility();
        }

        int aveAbilityOnTeam = totalAthleticAbility / numOfPlayers;
        Random rand = new Random();
        int numberBetween5and9 = rand.nextInt(5) + 5;

        if (numberBetween5and9 > aveAbilityOnTeam) {
            sport.setGamesLost(sport.getGamesLost() + 1);
            NewsManager.createNews(runId, hoursAlive, sport.getName() + " just lost a game.", NewsType.SPORTS_NEWS, NewsLevel.BAD_NEWS);
        } else {
            sport.setGamesWon(sport.getGamesWon() + 1);
            NewsManager.createNews(runId, hoursAlive, sport.getName() + " just won a game!", NewsType.SPORTS_NEWS, NewsLevel.GOOD_NEWS);
        }
        SportManager rep = new SportManager();
        rep.sportRep(sport, runId);
    }

    /**
     * Set the reputation of a sports team based on wins and loses.
     *
     * @param sport
     * @param runId
     */
    public void sportRep(SportModel sport, String runId) {
        if (sport.getGamesWon() > sport.getGamesLost()) {
            sport.setReputation(sport.getReputation() + 5);
        } else if (sport.getGamesWon() < sport.getGamesLost()) {
            sport.setReputation(sport.getReputation() - 5);
        }

        int rep = sport.getReputation();
        rep = Math.max(rep, 0);
        rep = Math.min(rep, 100);
        sport.setReputation(rep);
    }
}

