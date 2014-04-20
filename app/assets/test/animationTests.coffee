QUnit.config.autostart = false;
QUnit.config.reorder = false
QUnit.config.testTimeout = 10 * 1000
window.ASSETS = "/assets/"
window.USER = {
    username: 'cbrahms'
    id: 'bla'
}

angular.injector(['ng', 'app.stratego']).invoke([
   '$q', '$log', 'StrategoFactory', 'StrategoActions', 'StrategoPieces', 
(Q, log, StrategoFactory, StrategoActions, StrategoPieces) ->    
    {StrategoAction, PlacePieceAction, MoveAction, AttackAction, ReplacePieceAction, CommitAction} = StrategoActions
    {StrategoPiece, RedPiece, BluePiece} = StrategoPieces

    board = new StrategoFactory.StrategoBoard({
        canvas: 'canvas'
        isRed: true    
    })

    board.init().then ->

        asyncTest "Can remove pending layer",  -> 
            board.setRunning()
            ok(board.pendingLayer is null)
            #board.enableAnimations()
            board.draw()
            start()
        asyncTest "Place scout at 1,2", ->
            action = new PlacePieceAction {
                x: 1
                y: 2
                piece: new RedPiece(StrategoPiece.SCOUT)
            }
            action.invoke(board).then ->
                equal(board.getPiece(1,2).value, StrategoPiece.SCOUT)
                ok(true)
                board.enableDragging()
                start()
        asyncTest "Move Scout at 1,2 to 9,2", ->
            action = new MoveAction {
                x: 1
                y: 2
                newX: 9
                newY: 2
            }
            action.invoke(board).then ->
                equal(board.getPiece(9,2).value, StrategoPiece.SCOUT)
                start()
        asyncTest "Attack a spy with scout", ->
            action = new PlacePieceAction {
                x: 9
                y: 9
                piece: new BluePiece(13)
            }
            action.invoke(board)
                .then ->
                    board.draw()
                    action = new AttackAction {
                        x: 9
                        y: 2
                        newX: 9
                        newY: 9
                        attacker: new RedPiece(StrategoPiece.SCOUT)
                        defender: new BluePiece(StrategoPiece.SPY)
                        result: "DEFENDER_DIES"
                        user: USER
                    }
                    action.invoke(board)
                .then ->
                    equal(board.getPiece(9, 9).value, StrategoPiece.SCOUT)
                    equal(board.getSideboardCount(new BluePiece(StrategoPiece.SPY)), 2)
                    start()
        asyncTest "Attack a bomb with scout", ->
            action = new PlacePieceAction {
                x: 1, 
                y: 9
                piece: new BluePiece(13)
            }
            action.invoke(board)
                .then ->
                    action = new AttackAction {
                        x: 9
                        y: 9
                        newX: 1
                        newY: 9
                        defender: new BluePiece(StrategoPiece.BOMB)
                        attacker: new RedPiece(StrategoPiece.SCOUT)
                        user: USER
                        result: "ATTACKER_DIES"
                    }
                    action.invoke(board)
                .then ->
                    equal(board.getPiece(1, 9).value, 13)
                    equal(board.getSideboardCount(new BluePiece(StrategoPiece.BOMB)), 6)
                    equal(board.getSideboardCount(new RedPiece(StrategoPiece.SCOUT)), 8)
                    start()
        asyncTest "Attack a scout with a scout", ->
            action = new PlacePieceAction {
                x: 1,
                y: 1
                piece: new RedPiece(StrategoPiece.SCOUT)
                user: USER
            }
            action.invoke(board)
                .then ->
                    equal(board.getPiece(1, 1).value, StrategoPiece.SCOUT)
                    action = new AttackAction {
                        x: 1
                        y: 1
                        newX: 1
                        newY: 9
                        attacker: new RedPiece(StrategoPiece.SCOUT)
                        defender: new BluePiece(StrategoPiece.SCOUT)
                        result: "BOTH_DIE"
                        user: USER
                    }
                    action.invoke(board)
                .then ->
                    equal(board.getPiece(1, 9), StrategoPiece.Empty)
                    equal(board.getPiece(1, 1), StrategoPiece.Empty)
                    start()
        QUnit.start()
])