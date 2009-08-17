package com.google.yaw.admin;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.yaw.Util;
import com.google.yaw.model.Assignment;
import com.google.yaw.model.Assignment.AssignmentStatus;

public class MutateAssignment extends HttpServlet {

    private static final Logger log = Logger.getLogger(UpdateSubmission.class
                    .getName());

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        
        PersistenceManagerFactory pmf = Util.getPersistenceManagerFactory();
        PersistenceManager pm = pmf.getPersistenceManager();
        
        try {
            String id = req.getParameter("id");
            String description = req.getParameter("description");
            String category = req.getParameter("category");
            String status = req.getParameter("status");
            String operation = req.getParameter("oper");
            
            if (Util.isNullOrEmpty(operation)) {
                throw new IllegalArgumentException("'oper' parameter is null or empty.");
            }
            if (Util.isNullOrEmpty(description)) {
                throw new IllegalArgumentException("'description' parameter is null or empty.");
            }
            if (Util.isNullOrEmpty(category)) {
                throw new IllegalArgumentException("'category' parameter is null or empty.");
            }
            if (Util.isNullOrEmpty(status)) {
                throw new IllegalArgumentException("'status' parameter is null or empty.");
            }
            
            if (operation.equals("add")) {
                // This is an attempt to add a new Assignment.
                log.fine(String.format("Attempting to persist Assignment with description '%s', " +
                                "category '%s', and status '%s'...", description, category,
                                status));
                Assignment assignment = new Assignment(description, category,
                                AssignmentStatus.valueOf(status));
                Util.persistJdo(assignment);
                log.fine(String.format("...Assignment with id '%s' persisted.",
                                assignment.getId()));
                
                resp.setContentType("text/javascript");
                resp.getWriter().println(Util.GSON.toJson(assignment));
            } else if (operation.equals("edit")) {
                // This is an attempt to edit an existing Assignment.
                if (Util.isNullOrEmpty(id)) {
                    throw new IllegalArgumentException("'id' parameter is null or empty.");
                }
                
                String filters = "id == id_";
                Query query = pm.newQuery(Assignment.class, filters);
                query.declareParameters("String id_");

                List<Assignment> list = (List<Assignment>)query.executeWithArray(
                                new Object[] { id });

                if (list.size() > 0) {
                    Assignment assignment = list.get(0);
                    assignment = pm.detachCopy(assignment);

                    assignment.setDescription(description);
                    assignment.setCategory(category);
                    assignment.setStatus(Assignment.AssignmentStatus.valueOf(status));

                    log.fine(String.format("Attempting to update Assignment with id '%s'...", id));
                    Util.persistJdo(assignment);
                    log.fine(String.format("...Assignment with id '%s' updated.", id));

                    resp.setContentType("text/javascript");
                    resp.getWriter().println(Util.GSON.toJson(assignment));
                } else {
                    throw new IllegalArgumentException(String.format("Could not find Assignment" +
                    		" with id '%s'.", id));
                }
            } else {
                throw new IllegalArgumentException(String.format("Operation type '%s' is not " +
                		"supported.", operation));
            }
        } catch (IllegalArgumentException e) {
            log.warning(e.toString());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } finally {
            pm.close();
        }
    }
}