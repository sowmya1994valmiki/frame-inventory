import type {
  CommercialDetails,
  CreateFrameRequest,
  Frame,
  FrameFieldError,
  IntegrationDetails,
  TechnicalDetails,
  UpdateFrameRequest,
} from '../api/frames'

export type FrameFormValues = {
  frameId: string
  mediaType: string
  format: string
  environment: string
  status: string
  statusReason: string
  postcode: string
  postcodeArea: string
  postcodeDistrict: string
  postcodeSector: string
  postcodeUnit: string
  address: string
  region: string
  countryCode: string
  town: string
  longitude: string
  latitude: string
  distanceToClosestSchool: string
  rawLocationPoint: string
  locationId: string
  siteNumber: string
  inventorySiteNumber: string
  panelNumber: string
  station: string
  airport: string
  illuminationTypeId: string
  numberOfSlots: string
  sizeCode: string
  sizeGroupCode: string
  aspectRatioCode: string
  sizeCategory: string
  pixelHeight: string
  pixelWidth: string
  impactWeight: string
  productionRateCard: string
  legacyProductionRateCard: string
  pricingGrade: string
  priceEntityId: string
  premium: '' | 'true' | 'false'
  broadsignDisplayUnitId: string
  broadsignFrameId: string
  broadsignDomainId: string
  linkedFrameIds: string
}

export type FormField = keyof FrameFormValues
export type FormErrors = Partial<Record<FormField, string>>

export function emptyFrameForm(): FrameFormValues {
  return {
    frameId: '', mediaType: '', format: '', environment: '', status: 'LIVE', statusReason: '',
    postcode: '', postcodeArea: '', postcodeDistrict: '', postcodeSector: '', postcodeUnit: '',
    address: '', region: '', countryCode: '', town: '', longitude: '', latitude: '',
    distanceToClosestSchool: '', rawLocationPoint: '', locationId: '',
    siteNumber: '', inventorySiteNumber: '', panelNumber: '', station: '', airport: '',
    illuminationTypeId: '', numberOfSlots: '', sizeCode: '', sizeGroupCode: '', aspectRatioCode: '',
    sizeCategory: '', pixelHeight: '', pixelWidth: '', impactWeight: '', productionRateCard: '',
    legacyProductionRateCard: '', pricingGrade: '', priceEntityId: '', premium: '',
    broadsignDisplayUnitId: '', broadsignFrameId: '', broadsignDomainId: '', linkedFrameIds: '',
  }
}

export function frameToForm(frame: Frame): FrameFormValues {
  return {
    frameId: frame.frameId,
    mediaType: frame.mediaType ?? '',
    format: frame.format ?? '',
    environment: frame.environment ?? '',
    status: frame.status ?? '',
    statusReason: frame.statusReason ?? '',
    postcode: frame.location?.postcode ?? '',
    postcodeArea: frame.location?.postcodeArea ?? '',
    postcodeDistrict: frame.location?.postcodeDistrict ?? '',
    postcodeSector: frame.location?.postcodeSector ?? '',
    postcodeUnit: frame.location?.postcodeUnit ?? '',
    address: frame.location?.address ?? '',
    region: frame.location?.region ?? '',
    countryCode: frame.location?.countryCode ?? '',
    town: frame.location?.town ?? '',
    longitude: valueToString(frame.location?.longitude),
    latitude: valueToString(frame.location?.latitude),
    distanceToClosestSchool: valueToString(frame.location?.distanceToClosestSchool),
    rawLocationPoint: frame.location?.rawLocationPoint ?? '',
    locationId: frame.location?.locationId ?? '',
    siteNumber: frame.site?.siteNumber ?? '',
    inventorySiteNumber: frame.site?.inventorySiteNumber ?? '',
    panelNumber: frame.site?.panelNumber ?? '',
    station: frame.site?.station ?? '',
    airport: frame.site?.airport ?? '',
    illuminationTypeId: frame.technical?.illuminationTypeId ?? '',
    numberOfSlots: valueToString(frame.technical?.numberOfSlots),
    sizeCode: frame.technical?.sizeCode ?? '',
    sizeGroupCode: frame.technical?.sizeGroupCode ?? '',
    aspectRatioCode: frame.technical?.aspectRatioCode ?? '',
    sizeCategory: frame.technical?.sizeCategory ?? '',
    pixelHeight: valueToString(frame.technical?.pixelHeight),
    pixelWidth: valueToString(frame.technical?.pixelWidth),
    impactWeight: valueToString(frame.commercial?.impactWeight),
    productionRateCard: frame.commercial?.productionRateCard ?? '',
    legacyProductionRateCard: frame.commercial?.legacyProductionRateCard ?? '',
    pricingGrade: frame.commercial?.pricingGrade ?? '',
    priceEntityId: frame.commercial?.priceEntityId ?? '',
    premium: frame.commercial?.premium == null ? '' : String(frame.commercial.premium) as 'true' | 'false',
    broadsignDisplayUnitId: frame.integrations?.broadsignDisplayUnitId ?? '',
    broadsignFrameId: frame.integrations?.broadsignFrameId ?? '',
    broadsignDomainId: frame.integrations?.broadsignDomainId ?? '',
    linkedFrameIds: frame.integrations?.linkedFrameIds ?? '',
  }
}

export function validateFrameForm(values: FrameFormValues, mode: 'create' | 'edit'): FormErrors {
  const errors: FormErrors = {}
  if (mode === 'create') {
    required(errors, 'frameId', values.frameId, 'Frame ID is required.')
    if (values.frameId.trim().length > 64) errors.frameId = 'Frame ID must be 64 characters or fewer.'
  }
  required(errors, 'mediaType', values.mediaType, 'Media type is required.')
  required(errors, 'format', values.format, 'Format is required.')
  if (values.status !== 'LIVE' && values.status !== 'INACTIVE') errors.status = 'Select a status.'
  required(errors, 'postcode', values.postcode, 'Postcode is required.')
  required(errors, 'region', values.region, 'Region is required.')
  required(errors, 'town', values.town, 'Town is required.')
  required(errors, 'siteNumber', values.siteNumber, 'Site number is required.')

  decimal(errors, 'longitude', values.longitude, 'Longitude must be a number.')
  decimal(errors, 'latitude', values.latitude, 'Latitude must be a number.')
  decimal(errors, 'impactWeight', values.impactWeight, 'Impact weight must be a number.')
  integer(errors, 'distanceToClosestSchool', values.distanceToClosestSchool, 'School distance must be a whole number.')
  integer(errors, 'numberOfSlots', values.numberOfSlots, 'Number of slots must be a whole number.')
  integer(errors, 'pixelHeight', values.pixelHeight, 'Pixel height must be a whole number.')
  integer(errors, 'pixelWidth', values.pixelWidth, 'Pixel width must be a whole number.')
  return errors
}

export function apiErrorsToForm(errors: FrameFieldError[]): FormErrors {
  const fields: Record<string, FormField> = {
    frameId: 'frameId', mediaType: 'mediaType', format: 'format', status: 'status',
    'location.postcode': 'postcode', 'location.region': 'region', 'location.town': 'town',
    'site.siteNumber': 'siteNumber',
  }
  return errors.reduce<FormErrors>((result, error) => {
    const field = fields[error.field]
    if (field) result[field] = error.message
    return result
  }, {})
}

export function toCreateRequest(values: FrameFormValues): CreateFrameRequest {
  return { frameId: values.frameId.trim(), ...toWritableRequest(values) }
}

export function toUpdateRequest(values: FrameFormValues): UpdateFrameRequest {
  return toWritableRequest(values)
}

function toWritableRequest(values: FrameFormValues): UpdateFrameRequest {
  const technical: TechnicalDetails = {
    illuminationTypeId: optional(values.illuminationTypeId),
    numberOfSlots: optionalInteger(values.numberOfSlots),
    sizeCode: optional(values.sizeCode),
    sizeGroupCode: optional(values.sizeGroupCode),
    aspectRatioCode: optional(values.aspectRatioCode),
    sizeCategory: optional(values.sizeCategory),
    pixelHeight: optionalInteger(values.pixelHeight),
    pixelWidth: optionalInteger(values.pixelWidth),
  }
  const commercial: CommercialDetails = {
    impactWeight: optionalNumber(values.impactWeight),
    productionRateCard: optional(values.productionRateCard),
    legacyProductionRateCard: optional(values.legacyProductionRateCard),
    pricingGrade: optional(values.pricingGrade),
    priceEntityId: optional(values.priceEntityId),
    premium: values.premium === '' ? null : values.premium === 'true',
  }
  const integrations: IntegrationDetails = {
    broadsignDisplayUnitId: optional(values.broadsignDisplayUnitId),
    broadsignFrameId: optional(values.broadsignFrameId),
    broadsignDomainId: optional(values.broadsignDomainId),
    linkedFrameIds: optional(values.linkedFrameIds),
  }

  return {
    mediaType: values.mediaType.trim(),
    format: values.format.trim(),
    environment: optional(values.environment),
    status: values.status as 'LIVE' | 'INACTIVE',
    statusReason: optional(values.statusReason),
    location: {
      postcode: values.postcode.trim(),
      postcodeArea: optional(values.postcodeArea),
      postcodeDistrict: optional(values.postcodeDistrict),
      postcodeSector: optional(values.postcodeSector),
      postcodeUnit: optional(values.postcodeUnit),
      address: optional(values.address),
      region: values.region.trim(),
      countryCode: optional(values.countryCode),
      town: values.town.trim(),
      longitude: optionalNumber(values.longitude),
      latitude: optionalNumber(values.latitude),
      distanceToClosestSchool: optionalInteger(values.distanceToClosestSchool),
      rawLocationPoint: optional(values.rawLocationPoint),
      locationId: optional(values.locationId),
    },
    site: {
      siteNumber: values.siteNumber.trim(),
      inventorySiteNumber: optional(values.inventorySiteNumber),
      panelNumber: optional(values.panelNumber),
      station: optional(values.station),
      airport: optional(values.airport),
    },
    technical: hasValue(technical) ? technical : null,
    commercial: hasValue(commercial) ? commercial : null,
    integrations: hasValue(integrations) ? integrations : null,
  }
}

function required(errors: FormErrors, field: FormField, value: string, message: string) {
  if (!value.trim()) errors[field] = message
}

function decimal(errors: FormErrors, field: FormField, value: string, message: string) {
  const normalized = value.trim()
  if (normalized && (
    !/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(normalized) ||
    !Number.isFinite(Number(normalized))
  )) errors[field] = message
}

function integer(errors: FormErrors, field: FormField, value: string, message: string) {
  const normalized = value.trim()
  if (!normalized) return
  if (!/^[+-]?\d+$/.test(normalized)) {
    errors[field] = message
    return
  }
  const number = Number(normalized)
  if (number < -2147483648 || number > 2147483647) errors[field] = 'Value is outside the supported whole-number range.'
}

function optional(value: string): string | null {
  return value.trim() || null
}

function optionalNumber(value: string): number | null {
  return value.trim() ? Number(value.trim()) : null
}

function optionalInteger(value: string): number | null {
  return optionalNumber(value)
}

function hasValue(value: object): boolean {
  return Object.values(value).some((entry) => entry !== null)
}

function valueToString(value: number | null | undefined): string {
  return value == null ? '' : String(value)
}
