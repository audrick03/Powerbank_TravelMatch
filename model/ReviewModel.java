package model;

    public class ReviewModel {

        private final String reviewId;
        private final String accountName;
        private final String destinationName;
        private final int rating;
        private final String title;
        private final String comment;
        private final String createdDate;

        public ReviewModel(String reviewId, String accountName, String destinationName, int rating, String title, String comment, String createdDate) {
            this.reviewId = reviewId;
            this.accountName = accountName;
            this.destinationName = destinationName;
            this.rating = rating;
            this.title = title;
            this.comment = comment;
            this.createdDate = createdDate;
        }

        public String getReviewId() {
            return reviewId;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getDestinationName() {
            return destinationName;
        }
        public int getRating() {
            return rating;
        }

        public String getTitle() {
            return title;
        }

        public String getComment() {
            return comment;
        }

        public String getCreatedDate() {
            return createdDate;
        }
    }