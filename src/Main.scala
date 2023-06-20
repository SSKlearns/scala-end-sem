import org.apache.spark.sql.{SparkSession, DataFrame}
import scala.io.Source
import scala.util.Try
object Main {

  def main(args: Array[String]): Unit = {
    worldCupMatches()
  }

  def worldCups(): Unit = {
    // Read the CSV file into a DataFrame
    val filePath = "/home/SSK/Datasets/FIFA/WorldCups.csv"

    // Read the FIFA dataset file
    val fileSource = Source.fromFile(filePath)
    val lines = fileSource.getLines().toList
    fileSource.close()

    // Remove the header line from the dataset
    val data = lines.tail

    // Task 1: Calculate the total number of matches
    val totalWorldCups = data.size
    println("Total Matches: ", totalWorldCups)

    // Task 2: Find the average goals scored per match
    val totalGoals = data.map(_.split(",")(6).toDouble)
    val totalMatches = data.map(_.split(",")(8).toInt)
    val averageGoalsPerYear = totalGoals.zip(totalMatches).map { case (a, b) => a / b }
    println("Average Goals per Year: ", averageGoalsPerYear)

    // Task 3: Identify the years with the highest and lowest average goals for the season
    val goalsThatYear = data.groupBy(_.split(",")(0)).mapValues { matches =>
      val goals = matches.map(_.split(",")(6).toDouble)
      val totalMatches = matches.map(_.split(",")(8).toInt)
      val averageGoalsPerYear = goals.zip(totalMatches).map { case (a, b) => a / b }
      averageGoalsPerYear.sum / averageGoalsPerYear.size
    }
    println(goalsThatYear)
    val highestAverageGoals = goalsThatYear.maxBy(_._2)
    val lowestAverageGoals = goalsThatYear.minBy(_._2)
    println("Year with Highest Average Goals per Match:", highestAverageGoals)
    println("Year with Lowest Average Goals per Match:", lowestAverageGoals)

    // Task 4: Determine the tournament year with the most matches
    val matchesByYear = data.groupBy(_.split(",")(0)).mapValues{ matches =>
      matches.map(_.split(",")(8).toInt).sum / matches.size
    }
    val recentYearWithMostMatches = matchesByYear.maxBy(_._2)
    val recentYearWithLeastMatches = matchesByYear.minBy(_._2)
    println("Year with Most Matches:", recentYearWithMostMatches)
    println("Year with Least Matches:", recentYearWithLeastMatches)

    // Task 5: Find the team with the highest win rate

    // Create a map to store the number of wins for each team
    var winsByTeam = Map[String, Int]()

    // Iterate through the dataset and count the number of wins for each team
    for (line <- data) {
      val winner = line.split(",")(2)
      winsByTeam += (winner -> (winsByTeam.getOrElse(winner, 0) + 1))
    }

    // Find the team with the maximum number of wins
    val teamWithMaxWins = winsByTeam.maxBy(_._2)._1

    // Print the team with the maximum number of wins
    println(s"Team with the most wins: ${teamWithMaxWins} ${winsByTeam.get(teamWithMaxWins).sum}")
  }


  def worldCupMatches(): Unit = {

    val filePath = "/home/SSK/Datasets/FIFA/WorldCupMatches.csv"

    // Read the FIFA dataset file
    val fileSource = Source.fromFile(filePath)
    val lines = fileSource.getLines().toList
    fileSource.close()

    // Remove the header line from the dataset
    val data = lines.tail
    // Total number of matches
    val totalMatches = data.length
    println(s"Total number of matches: $totalMatches")

    val spark = SparkSession.builder().appName("DataAnalysis").master("spark://10.12.52.234:7077").getOrCreate()
    val df = spark.read.options(Map("inferSchema"->"true","delimiter"->",","header"->"true")).csv(filePath)
    df.show()
    df.printSchema()
    // Average goals scored per match
    val totalGoals = data.map(line => line.split(",")(6).toInt + line.split(",")(7).toInt).sum
    val averageGoalsPerMatch = totalGoals.toDouble / totalMatches
    println(s"Average goals scored per match: $averageGoalsPerMatch")

    // Teams with the highest and lowest average goals per match
    val goalsByTeam = data.flatMap(line => List((line.split(",")(5), line.split(",")(6).toInt), (line.split(",")(8), line.split(",")(7).toInt)))
    val goalsByTeamGrouped = goalsByTeam.groupBy(_._1)
    val averageGoalsByTeam = goalsByTeamGrouped.map { case (team, goals) =>
      val totalGoalsByTeam = goals.map(_._2).sum
      val averageGoalsByTeam = totalGoalsByTeam.toDouble / goals.length
      (team, averageGoalsByTeam)
    }
    val teamWithMaxAverageGoals = averageGoalsByTeam.maxBy(_._2)._1
    val teamWithMinAverageGoals = averageGoalsByTeam.minBy(_._2)._1
    println(s"Team with the highest average goals per match: $teamWithMaxAverageGoals")
    println(s"Team with the lowest average goals per match: $teamWithMinAverageGoals")

    // Tournament year with the most matches
    val matchesByYear = data.groupBy(_.split(",")(0))
    val yearWithMostMatches = matchesByYear.maxBy(_._2.length)._1
    println(s"Tournament year with the most matches: $yearWithMostMatches")

    // Team with the highest win rate
    val winsByTeam = data.groupBy(_.split(",")(2))
    val winRatesByTeam = winsByTeam.map { case (team, matches) =>
      val totalMatchesByTeam = matches.length
      val totalWinsByTeam = matches.count(_.split(",")(5) == team)
      val winRateByTeam = (totalWinsByTeam.toDouble / totalMatchesByTeam) * 100
      (team, winRateByTeam)
    }
    val teamWithHighestWinRate = winRatesByTeam.maxBy(_._2)._1
    println(s"Team with the highest win rate: $teamWithHighestWinRate")

    // Total number of goals scored by each team
    val goalsByTeamTotal = goalsByTeamGrouped.map { case (team, goals) =>
      val totalGoalsByTeam = goals.map(_._2).sum
      (team, totalGoalsByTeam)
    }
    println("Total number of goals scored by each team:")
    goalsByTeamTotal.foreach(println)

    // Teams with the most and fewest goals scored
    val teamWithMostGoals = goalsByTeamTotal.maxBy(_._2)._1
    val teamWithFewestGoals = goalsByTeamTotal.minBy(_._2)._1
    println(s"Team with the most goals scored: $teamWithMostGoals")
    println(s"Team with the fewest goals scored: $teamWithFewestGoals")

    // Most common tournament stage
    val tournamentStages = data.map(_.split(",")(2))
    val mostCommonStage = tournamentStages.groupBy(identity).maxBy(_._2.length)._1
    println(s"Most common tournament stage: $mostCommonStage")

    // Distribution of match outcomes
    val matchOutcomes = data.map(line => {
      val homeGoals = line.split(",")(6).toInt
      val awayGoals = line.split(",")(7).toInt
      if (homeGoals > awayGoals) "Home team win"
      else if (homeGoals < awayGoals) "Away team win"
      else "Draw"
    })
    val outcomeDistribution = matchOutcomes.groupBy(identity).mapValues(_.length)
    println("Distribution of match outcomes:")
    outcomeDistribution.foreach { case (outcome, count) =>
      println(s"$outcome: $count")
    }

    // Average goals scored by the home team and away team
    val homeGoalsTotal = data.map(_.split(",")(6).toInt).sum
    val awayGoalsTotal = data.map(_.split(",")(7).toInt).sum
    val averageHomeGoals = homeGoalsTotal.toDouble / totalMatches
    val averageAwayGoals = awayGoalsTotal.toDouble / totalMatches
    println(s"Average goals scored by the home team: $averageHomeGoals")
    println(s"Average goals scored by the away team: $averageAwayGoals")
  }
}