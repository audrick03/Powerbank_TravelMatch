package Controller;

import View.RecommendationView;
import java.awt.event.*;
import java.util.*;

public class RecommendationController {
    private RecommendationView view;

    public RecommendationController(RecommendationView view) {
        this.view = view;
        this.view.addBackListener(new BackListener());
        this.view.addLogoutListener(new LogoutListener());

        // Simple list of destinations (name + province only)
        List<String> destinations = Arrays.asList(
            "Batad Rice Terraces — Ifugao",
            "Batanes — Cagayan Valley",
            "Biri Island — Northern Samar",
            "Apo Reef Natural Park — Occidental Mindoro",
            "Mount Pulag — Benguet / Ifugao",
            "Sagada — Mountain Province",
            "Gigantes Islands — Iloilo",
            "Coron — Palawan",
            "Vigan City — Ilocos Sur",
            "Mount Dulang-Dulang — Bukidnon",
            "Mount Pinatubo — Zambales / Tarlac",
            "Siquijor — Central Visayas",
            "Siargao Island — Surigao del Norte",
            "El Nido — Palawan",
            "Cebu City — Cebu",
            "San Fernando City — La Union",
            "Davao City — Davao del Sur",
            "Intramuros — Manila",
            "Binondo — Manila",
            "La Mesa Watershed Reserve — Quezon City",
            "Poblacion — Makati City",
            "National Museum Complex — Manila",
            "Bonifacio Global City — Taguig",
            "Las Piñas–Parañaque Critical Habitat — Manila",
            "Ninoy Aquino Parks & Wildlife — Quezon City",
            "Ayala Triangle & Greenbelt — Makati City",
            "Manila Baywalk & Harbour Square — Manila"
        );

        view.showRecommendations(destinations);
    }

    class BackListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            view.showMessage("⬅ Going back to User Panel...");
            view.dispose();
        }
    }

    class LogoutListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            view.showMessage("👋 Logged out successfully!");
            view.dispose();
        }
    }
}
