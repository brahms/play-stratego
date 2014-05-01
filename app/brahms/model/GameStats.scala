package brahms.model

/**
 * Models the winners, losers, players, and drawers of a game
 * @param players
 * @param winners
 * @param losers
 * @param draws
 */
case class GameStats(val players: Seq[User] = Seq(),
                 val winners: Seq[User] = Seq(),
                 val losers:  Seq[User] = Seq(),
                 val draws:   Seq[User] = Seq(),
                 val game: Game)
