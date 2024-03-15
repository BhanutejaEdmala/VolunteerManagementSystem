package com.example.Vms.service;

import com.example.Vms.entities.Event;
import com.example.Vms.entities.Organisation;
import com.example.Vms.models.EventModel;
import com.example.Vms.models.OrganisationModel;

import java.util.List;

public interface OrganisationServiceInterface {
    OrganisationModel save(Organisation organisation);

    EventModel addEvent(int oid, int eid);

    String assignEvent(int vid, int eid, int oid);

    List<Event> viewEventsInOrganisation(int oid);

    String sendMessage(int vid, int oid, String message);

    String groupMessage(int oid, String message);

    String suggestVolunteers(int eid, int oid);

    String removeOrganization(int oid);

    String updateOrganisation(Organisation organisation, int oid);

    String closeEventForOrg(int eid, int oid);

    String removeVolunteer(int vid);

    List<String> viewMessagesOfVolunteers(int oid);

    Organisation get(int oid);
}