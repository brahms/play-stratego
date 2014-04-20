angular.module('app.stratego.actions', ['app.stratego.pieces'])
    .factory('StrategoActions', ['$log', 'StrategoPieces',
(log, StrategoPieces) ->
    {StrategoPiece} = StrategoPieces
    class StrategoAction
        constructor: ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"

    StrategoAction.fromJson = (json) ->
        action = null
        switch json.type
            when 'MoveAction' then action = new MoveAction({})
            when 'PlacePieceAction' then action = new PlacePieceAction({})
            when 'ReplacePieceAction' then action = new ReplacePieceAction({})
            when 'AttackAction' then action = new AttackAction({})
            when 'CommitAction' then action = new CommitAction({})
        action.fromJson(json)
        action

    class MoveAction extends StrategoAction
        constructor: (@x, @y, @newX, @newY) ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) -> {
                type: 'MoveAction'
                x: @x
                y: @y
                newX: @newX
                newY: @newY
            }
        fromJson: (json) ->
            @x = json.x
            @y = json.y
            @newX = json.newX
            @newY = json.newY
            @user = json.user
        toString: ->
            "MoveAction[user: #{@user}, x: #{@x}, y: #{@y} newX: #{@newX}, newY: #{@newY}"

    class AttackAction extends StrategoAction
        constructor: (@user, @x, @y, @newX, @newY) ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) -> {
                type: 'AttackAction'
                x: @x
                y: @y
                newX: @newX
                newY: @newY
        }
        fromJson: (json) ->
            @x = json.x
            @y = json.y
            @newX = json.newX
            @newY = json.newY
            @user = json.user
            @attacker = StrategoPiece.fromJson(json.attacker)
            @defender = StrategoPiece.fromJson(json.defender)
            @result = json.result
        toString: ->
            "AttackAction[user: #{@user}, \
            x: #{@x}, \
            y: #{@y} \
            newX: #{@newX}, \
            newY: #{@newY}, \
            attacker: #{@attacker}, \
            defender: #{@defender}, \
            result: #{@result}]"
    class PlacePieceAction extends StrategoAction
        constructor: ({user, x, y, piece}) ->
            @user = user
            @x = x
            @y = y
            @piece = piece
        isLegal: (board) ->
            log.debug("PlacePieceAction.isLegal")
            if board.phase != 'PLACE_PIECES'
                return false

            log.debug("PlacePieceAction.isLegal return true")

            true
        apply: (board) ->
            promise = board.placePiece(@piece, @x, @y)
            if (@piece.value != 13) then board.decrementSideboardCount(@piece)
            promise

        toJson: (json) -> {
            type: 'PlacePieceAction'
            x: @x
            y: @y
            piece: @piece.toJson()
        }
        fromJson: (json) -> 
            @x = json.x
            @y = json.y
            @piece = StrategoPiece.fromJson(json.piece)
            @user = json.user
        toString: ->
            "PlacePieceAction[user: #{@user}, x:#{@x} y:#{@y} piece:#{@piece}]"

    class ReplacePieceAction extends StrategoAction
        @constructor: (@x, @y, @newX, @newY) ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) -> {
            type: 'ReplacePieceAction'
            x: @x
            y: @y
            newX: @newX
            newY: @newY
        }
        fromJson: (json) -> 
            @x = json.x
            @y = json.y
            @newX = json.newX
            @newY = json.newY
        toString: ->
            "ReplacePieceAction[user: #{@user}, x:#{@x} y:#{@y} newX: #{@newX}, newY: #{@newY}]"


    class CommitAction extends StrategoAction
        constructor: ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) -> {}
        fromJson: (json) ->
            @user = json.user

        toString: ->
            "CommitAction[user: #{@user}]"

    {
        StrategoAction: StrategoAction
        MoveAction: MoveAction
        AttackAction: AttackAction
        ReplacePieceAction: ReplacePieceAction
        PlacePieceAction: PlacePieceAction
        CommitAction: CommitAction
    }

])