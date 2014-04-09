angular.module('app.stratego', [
    'app.stratego.piece', 
    'app.stratego.board', 
    'app.stratego.square',
    'app.stratego.functions'
]).factory('StrategoFactory', [
    '$log', 
    '$http', 
    'StrategoPiece', 
    'StrategoBoard', 
    'StrategoSquare',
(log, http, StrategoPiece, StrategoBoard, StrategoSquare) ->
    class StrategoAction
        constructor: ->
        isLegal: ->
            throw "isLegal Not implemented"
        apply: ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"

    StrategoAction.fromJson = (obj) ->
        ""

    class MoveAction extends StrategoAction
        constructor: ->

    class AttackAction extends StrategoAction
        isLegal: ->
            throw "isLegal Not implemented"
        apply: ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"
    class PlacePieceAction extends StrategoAction
        isLegal: ->
            throw "isLegal Not implemented"
        apply: ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"
    class ReplacePieceAction extends StrategoAction
        isLegal: ->
            throw "isLegal Not implemented"
        apply: ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"

    class StrategoModel
        constructor: (@gameId, @user) ->
            log.debug "StrategoModel constructor"
            @boardMatrix = [
                    [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0,-1,-1, 0, 0,-1,-1, 0, 0,-1]
                    [-1, 0, 0,-1,-1, 0, 0,-1,-1, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]
                ]
            @currentPlayer = @user
        getPiece: (x, y) =>



    class StrategoView
        constructor: (@model, @ctrl) ->
            log.debug('StrategoView constructor');
            @board = new StrategoBoard('canvas', @model)

    class StrategoController
        constructor: (@model) ->
            log.debug('StrategoController constructor');
        invoke: (action) ->
            log.debug('StrategoController invoke');

    return {
            StrategoModel: StrategoModel
            StrategoController: StrategoController
            StrategoView: StrategoView
        }

])