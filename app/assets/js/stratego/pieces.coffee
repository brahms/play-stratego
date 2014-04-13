angular.module('app.stratego.pieces', ['app.stratego.functions'])
    .factory('StrategoPieces', ['$log', '$window', 'StrategoFunctions',
(log, $window, StrategoFunctions) ->    
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions

    imagesPromises = []

    images = {}

    class StrategoPiece
        constructor: (@color, @value, @board) ->
            @square = null
        getImageKey: => 
            "#{@color}#{@value}"
        toString: =>
            "StrategoPiece[#{@getImageKey()} #{@image?.x()}/#{@image?.y()}]"
        setSquare: (@square) ->
            log.debug("setSquare: #{@square}")

        addToLayer: (layer)->
            if not @square then throw "No square defined"
            log.debug("Adding to #{layer.toString()} using x: #{@square.layerX} y: #{@square.layerY}")
            imgElement = new Image()
            imgElement.src = @getImageUrl()
            imgElement.onload = => 
                log.debug("onload: #{@getImageUrl()}")
                @image = new Kinetic.Image {
                    image: imgElement
                    draggable: true
                    x: @square.layerX
                    y: @square.layerY
                    width: SQUARE_HEIGHT,
                    height: SQUARE_WIDTH
                }
                layer.add(@image)
                layer.draw()
                @image.moveToTop()
                @image.on 'dragstart', =>
                    log.debug("dragstart #{@}}")
                @image.on 'dragend', =>
                    log.debug("dragend #{@}}")
                    center = @getCenter()
                    square = @board.getSquareForLayerPoint(center.x, center.y)
                    log.debug("dragged to: #{square}")
                    $window.setTimeout((=>

                        log.debug("reset #{@}")
                        @reset()
                    ), 1000)

        getCenter: => {
                x: (@image.x() + (@image.width()/2))
                y: (@image.y() + (@image.height()/2))
            }
        getImageUrl: =>
            "#{ASSETS}images/stratego/#{@color}#{@value}.png"
        reset: =>
            @image.x(@square.layerX)
            @image.y(@square.layerY)
            @image.getLayer().draw()


    class BluePiece extends StrategoPiece
        constructor: (value, board) ->
            super('b', value, board)
    class RedPiece extends StrategoPiece
        constructor: (value, board) ->
            super('r', value, board)


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


    return {
        StrategoPiece: StrategoPiece
        BluePiece: BluePiece
        RedPiece: RedPiece
    }
])