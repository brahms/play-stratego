angular.module('app.stratego.actions', [])
    .factory('StrategoActions', ['$log',
(log) ->
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

    StrategoAction.fromJson = (obj) ->
        ""

    class MoveAction extends StrategoAction
        constructor: ->

    class AttackAction extends StrategoAction
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"
    class PlacePieceAction extends StrategoAction
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"
    class ReplacePieceAction extends StrategoAction
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"

    class CommitAction extends StrategoAction
        constructor: ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"

    {
        StrategoAction: StrategoAction
        MoveAction: MoveAction
        AttackAction: AttackAction
        ReplacePieceAction: ReplacePieceAction
        PlacePieceAction: PlacePieceAction
        CommitAction: CommitAction
    }

])