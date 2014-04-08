angular.module('app.stratego.board', ['app.stratego.square'])
    .factory('StrategoBoard', ['StrategoSquare', 
(StrategoSquare) ->
    class StrategoBoard
        constructor: (@canvas) ->
            stage = new Kinetic.Stage({
                            container: @canvas
                            height: 700
                            width: 1000
                            })
            bgImageObject = new Image()
            backgroundLayer = new Kinetic.Layer()
            @offsetx = (@stage.getWidth() / 2) - (525/2)
            bgImageObject.src = "/images/stratego/board.jpg"
            bgImageObject.onload = () => 
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
                    @log.debug x + "/" + y
                squaresLayer = new Kinetic.Layer()
                for x in [1..10]
                    for y in [1..10]
                        new StrategoSquare(x, y, @, squaresLayer)
                stage.add squaresLayer
])