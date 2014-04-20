angular.module('app.stratego.pieces', ['app.stratego.functions'])
    .factory('StrategoPieces', ['$log', '$q', '$window', 'StrategoFunctions',
(log, Q, $window, StrategoFunctions) ->    
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions

    imagesPromises = []

    images = {}

    class StrategoPiece
        constructor: (color, value) ->
            if !color? then throw "StrategoPiece color null"
            if !value? then throw "StrategoPiece value null"
            @color = color
            @value = value
        addToLayer: ({layer, x, y, emitter}) ->
            log.debug("#{@} addToLayer")
            if !layer? then throw "StrategoPiece layer null"
            if !emitter? then throw "StrategoPiece emitter null"
            @emitter = emitter
            defer = Q.defer()
            imgElement = new Image()
            imgElement.src = @_getImageUrl()
            imgElement.onload = => 
                @image = new Kinetic.Image {
                    image: imgElement
                    draggable: false
                    x: x
                    y: y
                    width: SQUARE_HEIGHT,
                    height: SQUARE_WIDTH
                }
                layer.add(@image)
                layer.draw()
                log.debug("#{@} Loaded")
                @image.on('dragend', @_onMove)
                log.debug "#{@} loaded, resolving promise"
                defer.resolve()
            defer.promise
        getImageKey: => 
            "#{@color}#{@value}"
        toString: =>
            "StrategoPiece[#{@getImageKey()} #{@image?.x()}/#{@image?.y()}]"
        setSquare: (@square) ->
            log.debug("setSquare: #{@square}")
        getCenter: => {
                x: (@image.x() + (@image.width()/2))
                y: (@image.y() + (@image.height()/2))
            }
        _getImageUrl: =>
            "#{ASSETS}images/stratego/#{@color}#{@value}.png"
        reset: =>
            log.debug("#{@} reset")
            @image.x(@square.layerX)
            @image.y(@square.layerY)
        draggableOn: ->
            @image.draggable(true)
        draggableOff: ->
            @image.draggable(false)
        kill: () ->
            log.debug("#{@} kill")
            layer.remove(image)

        _onMove: =>
            log.debug("#{@} _onMove: (#{@image.x()}, #{@image.y()}")
            @emitter.emit('move', @)


    class BluePiece extends StrategoPiece
        constructor: (value) ->
            super('b', value)
        toJson: => {
            value: @value
            type: 'RedPiece'
        }
    class RedPiece extends StrategoPiece
        constructor: (value) ->
            super('r', value)
        toJson: => {
            value: @value
            type: 'BluePiece'
        }


    StrategoPiece.SPY = 1
    StrategoPiece.SCOUT = 2
    StrategoPiece.MINER = 3
    StrategoPiece.SERGENT = 4
    StrategoPiece.LIEUTENANT = 5
    StrategoPiece.CAPTAIN = 6
    StrategoPiece.MAJOR = 7
    StrategoPiece.COLONEL = 8
    StrategoPiece.GENERAL = 9
    StrategoPiece.MARSHAL = 10
    StrategoPiece.BOMB = 11
    StrategoPiece.FLAG = 12
    StrategoPiece.MINVAL = StrategoPiece.SPY
    StrategoPiece.MAXVAL = StrategoPiece.FLAG
    StrategoPiece.BOUNDARY = -1
    StrategoPiece.Empty = 0
    StrategoPiece.UNKNOWN = 13

    StrategoPiece.fromJson = (json) ->
        log.debug("StrategoPiece.fromJson: #{angular.toJson(json)}")
        piece = null
        switch json.type
            when "RedPiece" then piece = new RedPiece(json.value)
            when "BluePiece" then piece = new BluePiece(json.value)
            else throw "StrategoPiece.fromJson cannot parse: #{angular.toJson(json)}"
    return {
        StrategoPiece: StrategoPiece
        BluePiece: BluePiece
        RedPiece: RedPiece
    }
])