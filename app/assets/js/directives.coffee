angular.module('app.directives', [])
    .directive('appVersion', ['version', (version) ->
        (scope, elm, attrs) ->
            elm.text(version)
    ])
    .directive('equals', [()->
        {
            restrict: 'A'
            require: '?ngModel'
            link: (scope, elem, attrs, ngModel) ->
                if (!ngModel) then return

                validate = () ->
                    val1 = ngModel.$viewValue
                    val2 = attrs.equals
                    ngModel.$setValidity('equals', val1 == val2)

                scope.$watch(attrs.ngModel, () ->
                    validate()
                )
                attrs.$observe('equals', (val) ->
                    validate()
                )

        }
    ])
    .directive('ngUnique', ['$http', '$log', (http, log) -> 
        {
            restrict: 'A'
            require: 'ngModel'
            link: (scope, elem, attrs, ngModel) ->
                if (!ngModel) then return

                elem.on 'blur', (evt) ->
                    scope.$apply () ->
                        val = elem.val()
                        log.debug("NG-UNIQUE: #{val}")
                        ajaxConfig = {
                            method: 'POST'
                            url: '/api/validate/username'
                            data: {
                                username: val
                            }
                        }
                        http(ajaxConfig)
                            .success((data, status, headers, config) ->
                                ngModel.$setValidity('unique', data.status)
                            )
        }

    ])