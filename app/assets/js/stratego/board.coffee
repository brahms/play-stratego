angular.module('app.stratego.board', ['app.stratego.square', 'app.stratego.pieces', 'app.stratego.functions'])
    .factory('StrategoBoard', ['$log', 'StrategoSquare', 'StrategoFunctions', 'StrategoPieces',
(log, StrategoSquare, StrategoFunctions, StrategoPieces) ->
    Boundary = -1
    Empty = 0
    {StrategoPiece, BluePiece, RedPiece} = StrategoPieces
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions

    class StrategoBoard
        constructor: (@canvas) ->
        registerController: (@ctrl) ->
            log.debug "Ctrl registered: #{@ctrl}"
        init: ->
            log.debug "Creating board at element: #{@canvas}"
            stage = new Kinetic.Stage({
                            container: @canvas
                            height: 600
                            width: 700
                })
            @stage = stage
            @matrix = [
                [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1] #x=0
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=1
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=2
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=3
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=4
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=5
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=6
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=7
                [-1, 0, 0, 0, 0,-1,-1, 0, 0, 0, 0,-1] #x=8
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=9
                [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1] #x=10
                [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1] #x=11
            ]   #y0 y1 y2 y3 y4 y5 y6 y7 y8 y9 y10 y11


            backgroundLayer = new Kinetic.Layer()    
            @offsetx = (@stage.getWidth() / 2) - (525/2)

            bgImageObject = new Image()
            bgImageObject.src = "#{ASSETS}images/stratego/board.jpg"
            bgImageObject.onload = () =>
                log.debug('boardImage loaded') 
                
                bgImage = new Kinetic.Image {
                    x: @offsetx
                    y: 0
                    image: bgImageObject
                    width: 450
                    height: 600
                    visible: true
                }
                backgroundLayer.add bgImage
                @stage.add backgroundLayer
                bgImage.on 'mousemove', () ->
                    x = stage.getPointerPosition().x - bgImage.getPosition().x;
                    y = stage.getPointerPosition().y - bgImage.getPosition().y;
                    # log.debug x + "/" + y
                squaresLayer = new Kinetic.Layer()
                for x in [1..10]
                    for y in [1..10]
                        if (@matrix[x][y] == Empty)
                            @matrix[x][y] = new StrategoSquare(x, y, @, squaresLayer)
                stage.add squaresLayer
                @matrix[1][10].initPiece(new BluePiece(StrategoPiece.MINER, @))
                @matrix[3][10].initPiece(new BluePiece(StrategoPiece.UNKNOWN, @))

        placePiece: (piece, x, y) ->
        killPiece: (piece, x, y) -> 
        movePiece: (piece, x, y) ->
        attackPiece: (attacker, defender) ->
        getSquareForLayerPoint: (layerX, layerY) ->
            for x in [1..10]
                for y in [1..10]
                    square = @matrix[x][y]
                    if (square instanceof StrategoSquare and square.intersects(layerX,layerY))
                        log.debug("Intersection found with: #{square}")
                        return square
            return null

        toString: ->
            "Board[]"

])