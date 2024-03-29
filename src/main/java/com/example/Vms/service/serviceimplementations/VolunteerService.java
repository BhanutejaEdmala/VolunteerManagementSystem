package com.example.Vms.service.serviceimplementations;
import com.example.Vms.conversions.EntityToModel;
import com.example.Vms.entities.Event;
import com.example.Vms.entities.Organisation;
import com.example.Vms.entities.User;
import com.example.Vms.entities.Volunteer;
import com.example.Vms.models.EventModel;
import com.example.Vms.models.OrganisationModel;
import com.example.Vms.models.VolunteerModel;
import com.example.Vms.repositories.UserRepo;
import com.example.Vms.repositories.EventRepo;
import com.example.Vms.repositories.OrganisationRepo;
import com.example.Vms.repositories.VolunteerRepo;
import com.example.Vms.service.serviceinterfaces.VolunteerServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class VolunteerService implements VolunteerServiceInterface {
    @Autowired
    VolunteerRepo volunteerRepo;
    @Autowired
    OrganisationRepo organisationRepo;
    @Autowired
    EventRepo eventRepo;
    @Autowired
    UserRepo userRepo;
    @Autowired
    EntityToModel entityToModel;
    @Autowired
    OrganisationService organisationService;
    public String add(String userName,int organisationId){
       if(organisationRepo.existsById(organisationId)&&userRepo.existsByName(userName)) {
           Organisation organisation = organisationRepo.findById(organisationId).orElse(null);
           User user = userRepo.findByName(userName).orElse(null);
           if(null!=organisation&&null!=user&&!(user.getOrganisations().contains(organisation))) {
               user.getOrganisations().add(organisation);
                   Volunteer volunteer = new Volunteer(user);
                   volunteer.setUser(user);
                   user.getVolunteers().add(volunteer);
                Set<Organisation> organisationList =   volunteer.getOrganisations();
                organisationList.add(organisation);
                volunteer.setOrganisations(organisationList);
                   organisation.getVolunteers().add(volunteer);
                  // userRepo.save(user);
                   organisationRepo.save(organisation);
               LocalDateTime currentDateTime = LocalDateTime.now();
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
               String formattedDateTime = currentDateTime.format(formatter);
                   user.getMessages().add("Your Joining Request is Approved By "+organisation.getName()+" "+formattedDateTime);
                   userRepo.save(user);
               return "You've Registered Successfully In "+organisation.getName();
           }
           else if(null!=user&&null!=organisation) {
               user.getMessages().add("you already registered as a volunteer in "+organisation.getName());
                userRepo.save(user);
           }
       }
       return "Check The Data You've Entered";
    }
   public List<OrganisationModel> findOrganisationByLoc(String location){
       return organisationRepo.findAll().stream().filter(i->i.getAddress().equals(location)).map(entityToModel::organisationToOrganisationModel).toList();
   }
   public List<VolunteerModel> viewVolunteersInEvent(int eventId, int organisationId){
        if(eventRepo.existsById(eventId)&&organisationRepo.existsById(organisationId)){
            Event event = eventRepo.findById(eventId).orElse(null);
            assert null != event;
            if(event.getStatus().equals("closed"))
                return null;
            Organisation organisation = organisationRepo.findById(organisationId).orElse(null);
            return volunteerRepo.findAll().stream().filter(i->i.getEvents().contains(event)&&i.getOrganisations().contains(organisation)).map(entityToModel::volunteerToVolunteerModel).toList();
        }
        return null;
   }
   public Set<String> showMessages(int volunteerId){
        boolean flag = volunteerRepo.existsById(volunteerId);
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
        if(flag&&null!=volunteer){
            return Objects.requireNonNull(volunteerRepo.findById(volunteerId).orElse(null)).getMessages();
        }
        return null;
   }
   public String CompleteEvent(int volunteerId,int eventId,int organisationId){
        Event event = eventRepo.findById(eventId).orElse(null);
        Organisation organisation = organisationRepo.findById(organisationId).orElse(null);
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
        if(null!=event&&null!=organisation){
               List<Event> events = organisation.getEvents();
               if(events.contains(event)){
                   if(null==volunteer)
                       return "Volunteer Doesn't Exist";
                   if(!(volunteer.getEvents().contains(event)))
                       return "event is not assigned to this volunteer";
                      User user = volunteer.getUser();
                      List<String> certificates = user.getCertificates();
                      certificates.add(user.getName()+"  has participated in the event "+event.getName()+" held in our organisation "+organisation.getName());
                      volunteer.getEvents().remove(event);
                      volunteerRepo.save(volunteer);
                      user.setCertificates(certificates);
                      userRepo.save(user);
                      return "You Have Completed The Event";
               }
               return "This Event Doesn't Exist In This Organisation";
        }
        return  "Check The Data You've Entered";
   }
   @Transactional
    public String leaveOrganisation(int volunteerId) {
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
        if (null!=volunteer) {
            List<Organisation> organisations = organisationRepo.findAll();
            List<Event> events = eventRepo.findAll();
            User user = volunteer.getUser();
            // Remove volunteer from events
            events.stream().filter(i->i.getVolunteerList().contains(volunteer)).forEach(event -> event.getVolunteerList().remove(volunteer));
            // Remove volunteer from organisations
            organisations.stream().filter(i->i.getVolunteers().contains(volunteer)).forEach(organisation -> organisation.getVolunteers().remove(volunteer));
            // Remove volunteer from user
            user.getVolunteers().remove(volunteer);
            // Save changes
            eventRepo.saveAll(events);
            organisationRepo.saveAll(organisations);
            userRepo.save(user);
            // Delete the volunteer from the repository
            volunteerRepo.delete(volunteer);
            return "Successfully left";
        }
        return "Volunteer Doesn't Exist";
    }
 public List<EventModel> viewEventsRegistered(int volunteerId){
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
            return null!=volunteer ?volunteer.getEvents().stream().filter(i->i.getStatus().equals("active")).distinct().map(entityToModel::eventToEventModel).toList():null;
 }
 public String sendMessageToOrganisation(int organisationId,int volunteerId,String message){
        Organisation organisation = organisationRepo.findById(organisationId).orElse(null);
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
        if(null!=organisation&&null!=volunteer){
            if(!(organisation.getVolunteers().contains(volunteer)))
                 return "You Are Not Part Of This Organisation";
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
            String formattedDateTime = currentDateTime.format(formatter);
            String msg = "volunteer id: "+volunteer.getVid()+" volunteer name: "+volunteer.getName()+" :message=>"+message+", time received "+formattedDateTime;
            List<String> messages = new ArrayList<>();
            if(null==organisation.getMessages()){
                organisation.setMessages(messages);
            }
            else{
                messages=organisation.getMessages();
            }
            messages.add(msg);
            organisationRepo.save(organisation);
            return "Sent";
        }
        return "Check The Data You've Entered";
 }
 public List<EventModel> searchEventsBySkill(int organiationId,String skill){
        Organisation organisation = organisationRepo.findById(organiationId).orElse(null);
     List<EventModel> events = new ArrayList<>();
        if(null!=organisation&&null!=organisation.getEvents()){
             events = organisation.getEvents().stream().filter(i->i.getSkills_good_to_have().contains(skill)).map(entityToModel::eventToEventModel).toList();
            return events;}
        return null;
 }
 public List<OrganisationModel> searchOrgByAddress(String address){
       List<OrganisationModel> organisations=  organisationRepo.findAll().stream().filter(i->i.getAddress().equals(address)).map(entityToModel::organisationToOrganisationModel).toList();
       if(!(CollectionUtils.isEmpty(organisations)))
           return organisations;
       return null;
 }
 public VolunteerModel get(int volunteerId){
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
        if(null!=volunteer)
            return entityToModel.volunteerToVolunteerModel(volunteer);
        return null;
 }
 public List<EventModel> eventsRegisteredByVolInOrg(String userName,String password,int volunteerId,int organisationId){
        Volunteer volunteer = volunteerRepo.findById(volunteerId).orElse(null);
     Organisation organisation = organisationRepo.findById(organisationId).orElse(null);
     if(null!=volunteer&&null!=organisation){
         return volunteerRepo.findEventsByVolunteerAndOrganisation(volunteerId,organisationId).stream().map(entityToModel::eventToEventModel).toList();
     }
     return null;
 }
 public String joinRequest(String userName,int organisationId){
        if(!(userRepo.existsByName(userName)))
            return "UserName Is Invalid";
        Organisation  organisation = organisationRepo.findById(organisationId).orElse(null);
        User user = userRepo.findByName(userName).orElse(null);
        List<String>  organisationMessages = new ArrayList<>();
        if(null!=organisation){
              organisationMessages =organisation.getMessages();
        organisationMessages.add("You Have Received A Joining Request From "+userName);
            assert user != null;
            organisation.getWaitingListUserIds().add(user.getUid());
        organisationRepo.save(organisation);
        return "Request Sent Successfully , You Will Receive An Approval Message Upon Accepting Your Joining Request";}
        else
            return "No Organisation Found";

 }
}