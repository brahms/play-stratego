angular.module('app.stratego.controller', ['app.stratego.actions'])
    .factory('StrategoController', ['$log', '$http', 'StrategoActions'
(log, http, StrategoActions) ->
    class StrategoController
        constructor: () ->
            log.debug('StrategoController constructor');
        invoke: (action) ->
            log.debug('StrategoController invoke');
        registerBoard: (@board) ->
            log.debug("Board registered: #{@board}")
])