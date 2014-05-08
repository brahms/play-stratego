package brahms.model.stratego;

public enum WinReason {

    /** When the flag has been captured **/
    CAPTURED_FLAG,

    /** WHen the other player forfeits due to timeout */
    OPPONENT_TIMED_OUT,

    /** When the other player cannot move **/
    OPPONENT_CANT_MOVE
}
