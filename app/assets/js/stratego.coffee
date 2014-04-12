angular.module('app.stratego', [
    'app.stratego.pieces', 
    'app.stratego.board', 
    'app.stratego.square',
    'app.stratego.functions',
    'app.stratego.actions',
    'app.stratego.controller'
]).factory('StrategoFactory', [
    '$log', 
    '$http', 
    'StrategoPieces', 
    'StrategoBoard', 
    'StrategoSquare',
    'StrategoActions',
    'StrategoController',
(log, http, StrategoPieces, StrategoBoard, StrategoSquare, StrategoActions, StrategoController) ->


    return {
            StrategoController: StrategoController
            StrategoBoard: StrategoBoard
        }

])