# Quiz Leaderboard System – Backend Integration Task

## 📌 Overview

This project implements a backend integration workflow to process quiz data from an external API and generate a final leaderboard.

The system simulates a real-world distributed environment where API responses may contain duplicate data. The goal is to correctly aggregate participant scores while handling such inconsistencies.

---

## 🎯 Objective

* Fetch quiz event data from an external API using multiple polling requests
* Handle duplicate data using a reliable deduplication strategy
* Compute total scores for each participant
* Generate a sorted leaderboard
* Submit the final result to the validator API

---

## ⚙️ Tech Stack

* Java (Core Java, no external dependencies)
* HTTP communication using `HttpURLConnection`
* Data structures: `HashMap`, `HashSet`, `ArrayList`

---

## 🔄 Approach

### 1. Polling the API

* The API is called **10 times** with `poll` values from `0` to `9`
* A **5-second delay** is maintained between each request to comply with constraints

### 2. Parsing Response

* Each API response contains a list of quiz events
* Each event has:

  * `roundId`
  * `participant`
  * `score`

### 3. Deduplication Logic

* A `HashSet` is used to track processed events
* Unique key format:

  ```
  roundId + "|" + participant
  ```
* If a duplicate event appears in later polls, it is ignored

### 4. Score Aggregation

* A `HashMap<String, Integer>` stores total scores per participant
* Scores are added only if the event is not a duplicate

### 5. Leaderboard Generation

* Participants are sorted:

  * Primarily by **total score (descending)**
  * Secondarily by **name (ascending)** for tie-breaking

### 6. Final Submission

* The computed leaderboard is sent to the API using a POST request
* Submission is performed **only once**

---

## 📊 Sample Output

```
Final Leaderboard:
Diana -> 470
Ethan -> 455
Fiona -> 440

Grand Total = 1365
```

---

## 🚀 How to Run

### 1. Compile

```bash
javac QuizLeaderboardSolver.java
```

### 2. Execute

```bash
java QuizLeaderboardSolver
```

---

## ⚠️ Important Notes

* The program enforces a strict **5-second delay** between API calls
* Duplicate API responses are handled correctly to avoid score inflation
* The solution is **idempotent** — repeated runs produce consistent results
* Submission should ideally be performed once, as per instructions

---

## 🧠 Key Learnings

* Handling duplicate data in distributed systems
* Designing idempotent workflows
* API integration and data consistency
* Efficient use of Java collections for real-time processing

---

## 📁 Repository Structure

```
.
├── QuizLeaderboardSolver.java
└── README.md
```

---

## ✅ Status

✔ Successfully implemented
✔ Correct leaderboard generated
✔ Stable and consistent output across runs
✔ API submission completed

---

## 🙌 Acknowledgment

This project was developed as part of the Bajaj Finserv Health Internship Assessment to demonstrate backend problem-solving and API integration skills.

---
