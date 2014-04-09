angular.module('app.stratego.board', ['app.stratego.square', 'app.stratego.functions'])
    .factory('StrategoBoard', ['$log', 'StrategoSquare', 'StrategoFunctions',
(log, StrategoSquare, StrategoFunctions) ->
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions

    class StrategoBoard
        constructor: (@canvas, @model) ->
            log.debug "Creating board at element: #{@canvas}"
            stage = new Kinetic.Stage({
                            container: @canvas
                            height: 700
                            width: 700
                })
            @stage = stage
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
                    width: 525
                    height: 700
                    visible: true
                }
                backgroundLayer.add bgImage
                @stage.add backgroundLayer
                bgImage.on 'mousemove', () ->
                    x = stage.getPointerPosition().x - bgImage.getPosition().x;
                    y = stage.getPointerPosition().y - bgImage.getPosition().y;
                    log.debug x + "/" + y
                squaresLayer = new Kinetic.Layer()
                for x in [1..10]
                    for y in [1..10]
                        new StrategoSquare(x, y, @, squaresLayer)
                stage.add squaresLayer

])